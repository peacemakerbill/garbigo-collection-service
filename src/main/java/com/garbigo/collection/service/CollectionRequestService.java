package com.garbigo.collection.service;

import com.garbigo.collection.client.WalletServiceClient;
import com.garbigo.collection.dto.CollectionRequestCreateRequest;
import com.garbigo.collection.dto.CollectionRequestResponse;
import com.garbigo.collection.dto.WalletPaymentRequest;
import com.garbigo.collection.dto.WalletPaymentResult;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.model.CollectionRequest;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.Location;
import com.garbigo.collection.model.PaymentStatus;
import com.garbigo.collection.model.SavedLocation;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.notification.MailService;
import com.garbigo.collection.repository.CollectionRequestRepository;
import com.garbigo.collection.util.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionRequestService {

    private static final String COLLECTOR_ROLE = "COLLECTOR";
    private static final String SERVICE_TYPE = "Garbage pickup";
    private static final DateTimeFormatter SCHEDULED_AT_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    private final CollectionRequestRepository collectionRequestRepository;
    private final UserSummaryService userSummaryService;
    private final SavedLocationService savedLocationService;
    private final MailService mailService;
    private final MongoTemplate mongoTemplate;
    private final WalletServiceClient walletServiceClient;

    @Value("${collections.cancellation-cutoff-hours}")
    private long cancellationCutoffHours;

    /**
     * Only used to STAMP currency onto a request the moment a price is set
     * (create with quotedPrice, or updateQuote) - once stored, every other
     * use (pay(), emails) reads the request's OWN currency field, not this
     * live config value. That's deliberate: if this default ever changes
     * later, a request already quoted under the old currency should stay
     * what it was actually quoted in, not get silently reinterpreted.
     */
    @Value("${payments.currency}")
    private String paymentsCurrency;

    public CollectionRequestResponse create(String clientId, CollectionRequestCreateRequest request) {
        CollectionRequest saved = collectionRequestRepository.save(
                CollectionRequest.builder()
                        .clientId(clientId)
                        .wasteType(request.getWasteType())
                        .status(CollectionStatus.PENDING)
                        .paymentStatus(PaymentStatus.UNPAID)
                        .scheduledAt(request.getScheduledAt())
                        .location(resolveLocation(clientId, request.getSavedLocationId(), request.getLocation()))
                        .notes(request.getNotes())
                        .quotedPrice(request.getQuotedPrice())
                        .currency(request.getQuotedPrice() != null ? paymentsCurrency : null)
                        .build()
        );
        notifyClientOfConfirmation(saved);
        return toResponse(saved);
    }

    public CollectionRequestResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    /** Used by RecurringScheduleRunner - bypasses the DTO/@Future validation since this is server-generated. */
    public CollectionRequestResponse createFromSchedule(
            String clientId, com.garbigo.collection.model.WasteType wasteType, Instant scheduledAt, Location location, String notes) {
        CollectionRequest saved = collectionRequestRepository.save(
                CollectionRequest.builder()
                        .clientId(clientId)
                        .wasteType(wasteType)
                        .status(CollectionStatus.PENDING)
                        .paymentStatus(PaymentStatus.UNPAID)
                        .scheduledAt(scheduledAt)
                        .location(location)
                        .notes(notes)
                        .build()
        );
        notifyClientOfConfirmation(saved);
        return toResponse(saved);
    }

    public Page<CollectionRequestResponse> getMine(String clientId, Pageable pageable) {
        return collectionRequestRepository.findByClientId(clientId, pageable).map(this::toResponse);
    }

    public Page<CollectionRequestResponse> getAssigned(String collectorId, Pageable pageable) {
        return collectionRequestRepository.findByCollectorId(collectorId, pageable).map(this::toResponse);
    }

    /** PENDING requests within radiusKm of the given point, nearest first - for a collector browsing nearby work. */
    public List<CollectionRequestResponse> findNearby(double latitude, double longitude, double radiusKm) {
        GeoJsonPoint point = new GeoJsonPoint(longitude, latitude);
        Query query = new Query(
                Criteria.where("location.coordinates").nearSphere(point).maxDistance(radiusKm * 1000)
        ).addCriteria(Criteria.where("status").is(CollectionStatus.PENDING));

        return mongoTemplate.find(query, CollectionRequest.class).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Client-driven: the requesting client picks their own collector (see
     * GET /collectors or /collectors/nearby). Allowed while PENDING (first
     * offer) or ASSIGNED (client changes their mind, or re-offers after a
     * decline) - blocked once work has actually started or the request is
     * otherwise terminal. Sets status to ASSIGNED, not an immediate
     * acceptance - the collector still has to accept (advance status via
     * PUT /status) or decline (PUT /decline).
     */
    public CollectionRequestResponse assign(String id, String clientId, String collectorId) {
        CollectionRequest existing = findOrThrow(id);
        if (!clientId.equals(existing.getClientId())) {
            throw new CustomException("Only the requesting client can assign a collector to this request");
        }
        if (existing.getStatus() != CollectionStatus.PENDING && existing.getStatus() != CollectionStatus.ASSIGNED) {
            throw new CustomException("Can only assign a collector while the request is PENDING or ASSIGNED (current: " + existing.getStatus() + ")");
        }

        UserSummary collector = userSummaryService.resolve(collectorId)
                .filter(user -> COLLECTOR_ROLE.equalsIgnoreCase(user.getRole()))
                .orElseThrow(() -> new CustomException("The selected user is not a collector: " + collectorId));

        existing.setCollectorId(collector.getId());
        existing.setStatus(CollectionStatus.ASSIGNED);
        existing.setLastDeclineReason(null);
        CollectionRequest saved = collectionRequestRepository.save(existing);
        notifyAssignment(saved, collector);
        return toResponse(saved);
    }

    /**
     * Collector declines an offered assignment (e.g. the price is too low) -
     * reverts to PENDING and clears collectorId, so the client can raise
     * quotedPrice (updateQuote) and re-assign, to the same or a different
     * collector, without creating a new request. Only valid while status is
     * ASSIGNED (i.e. before the collector has started); once IN_PROGRESS,
     * backing out is a cancellation decision for the client, not this.
     */
    public CollectionRequestResponse decline(String id, String collectorId, String reason) {
        CollectionRequest existing = findOrThrow(id);
        if (!collectorId.equals(existing.getCollectorId())) {
            throw new CustomException("Only the assigned collector can decline this request");
        }
        if (existing.getStatus() != CollectionStatus.ASSIGNED) {
            throw new CustomException("Can only decline a request that's awaiting acceptance (current: " + existing.getStatus() + ")");
        }

        UserSummary collector = userSummaryService.findById(collectorId).orElse(null);
        existing.setCollectorId(null);
        existing.setStatus(CollectionStatus.PENDING);
        existing.setLastDeclineReason(reason);
        CollectionRequest saved = collectionRequestRepository.save(existing);
        notifyDecline(saved, collector, reason);
        return toResponse(saved);
    }

    public CollectionRequestResponse updateStatus(String id, String collectorId, CollectionStatus newStatus) {
        CollectionRequest existing = findOrThrow(id);
        if (!collectorId.equals(existing.getCollectorId())) {
            throw new CustomException("Only the assigned collector can update this request's status");
        }
        existing.setStatus(newStatus);
        CollectionRequest saved = collectionRequestRepository.save(existing);
        if (newStatus == CollectionStatus.COMPLETED) {
            notifyCompletion(saved);
        }
        return toResponse(saved);
    }

    /** Client-controlled - a collector who thinks the price is too low uses decline(), not this. */
    public CollectionRequestResponse updateQuote(String id, String clientId, BigDecimal quotedPrice) {
        if (quotedPrice.signum() <= 0) {
            throw new CustomException("Price must be greater than zero");
        }
        CollectionRequest existing = findOrThrow(id);
        if (!clientId.equals(existing.getClientId())) {
            throw new CustomException("Only the requesting client can set the price for this request");
        }
        if (existing.getStatus() == CollectionStatus.COMPLETED || existing.getStatus() == CollectionStatus.CANCELLED) {
            throw new CustomException("Cannot update the price on a request that's already " + existing.getStatus());
        }
        existing.setQuotedPrice(quotedPrice);
        existing.setCurrency(paymentsCurrency);
        return toResponse(collectionRequestRepository.save(existing));
    }

    /**
     * Initiates payment to the assigned collector via garbigo-wallet-service.
     * That service has no designed API yet, so WalletServiceClient's
     * contract is a best-effort guess, not a confirmed integration - if this
     * fails outright (unreachable service), it surfaces as
     * feign.RetryableException -> GlobalExceptionHandler's 503. A negative
     * response that DOES come back sets paymentStatus to FAILED rather than
     * throwing, so the client can see and retry.
     *
     * Uses the request's OWN stored currency, not the live config default -
     * falls back to the current default only for a legacy record that
     * somehow has a price but no stored currency (predates this field).
     */
    public CollectionRequestResponse pay(String id, String clientId) {
        CollectionRequest existing = findOrThrow(id);
        if (!clientId.equals(existing.getClientId())) {
            throw new CustomException("Only the requesting client can pay for this request");
        }
        if (existing.getCollectorId() == null) {
            throw new CustomException("Assign a collector before paying");
        }
        if (existing.getQuotedPrice() == null) {
            throw new CustomException("No price has been set for this request yet");
        }
        if (existing.getPaymentStatus() == PaymentStatus.PAID) {
            throw new CustomException("This request has already been paid for");
        }
        if (existing.getStatus() == CollectionStatus.CANCELLED) {
            throw new CustomException("Cannot pay for a cancelled request");
        }

        existing.setPaymentStatus(PaymentStatus.PENDING);
        collectionRequestRepository.save(existing);

        String currency = existing.getCurrency() != null ? existing.getCurrency() : paymentsCurrency;

        WalletPaymentResult result = walletServiceClient.initiatePayment(
                WalletPaymentRequest.builder()
                        .payerId(clientId)
                        .payeeId(existing.getCollectorId())
                        .referenceId(existing.getId())
                        .amount(existing.getQuotedPrice())
                        .currency(currency)
                        .build()
        );

        existing.setPaymentStatus(result != null && result.isSuccess() ? PaymentStatus.PAID : PaymentStatus.FAILED);
        return toResponse(collectionRequestRepository.save(existing));
    }

    /** Blocked once a collector is actively on the job, or too close to scheduledAt - see collections.cancellation-cutoff-hours. */
    public void cancel(String id, String clientId, String reason) {
        CollectionRequest existing = findOrThrow(id);
        if (!clientId.equals(existing.getClientId())) {
            throw new CustomException("Only the requesting client can cancel this request");
        }
        if (existing.getStatus() == CollectionStatus.IN_PROGRESS || existing.getStatus() == CollectionStatus.COMPLETED) {
            throw new CustomException("Cannot cancel a request that's already " + existing.getStatus());
        }
        if (existing.getScheduledAt() != null
                && ChronoUnit.HOURS.between(Instant.now(), existing.getScheduledAt()) < cancellationCutoffHours) {
            throw new CustomException("Too close to the scheduled time to cancel - contact support instead");
        }
        existing.setStatus(CollectionStatus.CANCELLED);
        existing.setCancellationReason(reason);
        collectionRequestRepository.save(existing);
    }

    private CollectionRequest findOrThrow(String id) {
        return collectionRequestRepository.findById(id)
                .orElseThrow(() -> new CustomException("Collection request not found: " + id));
    }

    private Location resolveLocation(String clientId, String savedLocationId, com.garbigo.collection.dto.LocationRequest inline) {
        if (savedLocationId != null) {
            SavedLocation saved = savedLocationService.findOwnedByOrThrow(savedLocationId, clientId);
            return saved.getLocation();
        }
        if (inline != null) {
            return LocationMapper.toLocation(inline);
        }
        throw new CustomException("Provide either savedLocationId or location");
    }

    private void notifyClientOfConfirmation(CollectionRequest request) {
        userSummaryService.findById(request.getClientId()).ifPresent(client ->
                mailService.sendCollectionRequestConfirmation(
                        client.getEmail(),
                        client.preferredName(),
                        request.getId(),
                        request.getWasteType().name(),
                        formatScheduledAt(request.getScheduledAt()),
                        formatLocationSummary(request.getLocation()),
                        request.getQuotedPrice(),
                        request.getCurrency()
                ));
    }

    private void notifyAssignment(CollectionRequest request, UserSummary collector) {
        String scheduledAt = formatScheduledAt(request.getScheduledAt());
        String locationSummary = formatLocationSummary(request.getLocation());

        userSummaryService.findById(request.getClientId()).ifPresent(client ->
                mailService.sendCollectorAssignedToClient(
                        client.getEmail(),
                        client.preferredName(),
                        request.getId(),
                        SERVICE_TYPE,
                        collector.preferredName(),
                        scheduledAt,
                        locationSummary,
                        request.getQuotedPrice(),
                        request.getCurrency()
                ));

        mailService.sendJobAssignedToCollector(
                collector.getEmail(),
                collector.preferredName(),
                request.getId(),
                SERVICE_TYPE,
                scheduledAt,
                locationSummary,
                request.getQuotedPrice(),
                request.getCurrency()
        );
    }

    private void notifyDecline(CollectionRequest request, UserSummary collector, String reason) {
        userSummaryService.findById(request.getClientId()).ifPresent(client ->
                mailService.sendCollectorDeclined(
                        client.getEmail(),
                        client.preferredName(),
                        request.getId(),
                        SERVICE_TYPE,
                        collector == null ? "Your collector" : collector.preferredName(),
                        reason,
                        request.getQuotedPrice(),
                        request.getCurrency()
                ));
    }

    private void notifyCompletion(CollectionRequest request) {
        userSummaryService.findById(request.getClientId()).ifPresent(client ->
                mailService.sendRequestCompleted(
                        client.getEmail(),
                        client.preferredName(),
                        request.getId(),
                        SERVICE_TYPE,
                        true,
                        request.getQuotedPrice(),
                        request.getCurrency()
                ));
    }

    private String formatScheduledAt(Instant scheduledAt) {
        return scheduledAt == null ? "TBD" : SCHEDULED_AT_FORMAT.format(scheduledAt.atZone(ZoneOffset.UTC));
    }

    private String formatLocationSummary(Location location) {
        return location == null ? "TBD" : String.join(", ", location.getLocationName(), location.getAddress());
    }

    private CollectionRequestResponse toResponse(CollectionRequest entity) {
        return CollectionRequestResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .collectorId(entity.getCollectorId())
                .wasteType(entity.getWasteType())
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .scheduledAt(entity.getScheduledAt())
                .location(LocationMapper.toResponse(entity.getLocation()))
                .notes(entity.getNotes())
                .quotedPrice(entity.getQuotedPrice())
                .currency(entity.getCurrency())
                .cancellationReason(entity.getCancellationReason())
                .lastDeclineReason(entity.getLastDeclineReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}