package com.garbigo.collection.service;

import com.garbigo.collection.client.WalletServiceClient;
import com.garbigo.collection.dto.SewageRequestCreateRequest;
import com.garbigo.collection.dto.SewageRequestResponse;
import com.garbigo.collection.dto.WalletPaymentRequest;
import com.garbigo.collection.dto.WalletPaymentResult;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.exception.NotFoundException;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.Location;
import com.garbigo.collection.model.PaymentStatus;
import com.garbigo.collection.model.SavedLocation;
import com.garbigo.collection.model.SewageRequest;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.notification.MailService;
import com.garbigo.collection.repository.SewageRequestRepository;
import com.garbigo.collection.util.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/** Sewage/exhauster requests - same lifecycle as CollectionRequestService, different domain fields. */
@Service
@RequiredArgsConstructor
public class SewageRequestService {

    private static final String COLLECTOR_ROLE = "COLLECTOR";
    private static final String SERVICE_TYPE = "Sewage collection";
    private static final DateTimeFormatter SCHEDULED_AT_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    private final SewageRequestRepository sewageRequestRepository;
    private final UserSummaryService userSummaryService;
    private final SavedLocationService savedLocationService;
    private final MailService mailService;
    private final WalletServiceClient walletServiceClient;

    @Value("${collections.cancellation-cutoff-hours}")
    private long cancellationCutoffHours;

    /** See CollectionRequestService's identical field - same "stamp once, read from storage after" rule. */
    @Value("${payments.currency}")
    private String paymentsCurrency;

    public SewageRequestResponse create(String clientId, SewageRequestCreateRequest request) {
        SewageRequest saved = sewageRequestRepository.save(
                SewageRequest.builder()
                        .clientId(clientId)
                        .tankVolumeLiters(request.getTankVolumeLiters())
                        .urgency(request.getUrgency())
                        .accessNotes(request.getAccessNotes())
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

    /**
     * Visible to: the client who made it, the collector it's assigned to,
     * and ADMIN/SUPPORT. Unlike collections there's no nearby-jobs listing
     * for sewage, so an unassigned collector has no way to have discovered
     * one and no reason to read it. Anyone else gets the same 404 a
     * nonexistent ID gets, so this can't be used to probe which IDs exist.
     */
    public SewageRequestResponse getById(String id, String callerId, Set<String> callerRoles) {
        SewageRequest request = findOrThrow(id);
        if (!canView(request, callerId, callerRoles)) {
            throw new NotFoundException("Sewage request not found: " + id);
        }
        return toResponse(request);
    }

    private boolean canView(SewageRequest request, String callerId, Set<String> callerRoles) {
        if (callerRoles.contains("ADMIN") || callerRoles.contains("SUPPORT")) {
            return true;
        }
        return callerId.equals(request.getClientId()) || callerId.equals(request.getCollectorId());
    }

    public Page<SewageRequestResponse> getMine(String clientId, Pageable pageable) {
        return sewageRequestRepository.findByClientId(clientId, pageable).map(this::toResponse);
    }

    public Page<SewageRequestResponse> getAssigned(String collectorId, Pageable pageable) {
        return sewageRequestRepository.findByCollectorId(collectorId, pageable).map(this::toResponse);
    }

    /** See CollectionRequestService.assign() - same accept/decline-pending semantics. */
    public SewageRequestResponse assign(String id, String clientId, String collectorId) {
        SewageRequest existing = findOrThrow(id);
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
        SewageRequest saved = sewageRequestRepository.save(existing);
        notifyAssignment(saved, collector);
        return toResponse(saved);
    }

    /** See CollectionRequestService.decline(). */
    public SewageRequestResponse decline(String id, String collectorId, String reason) {
        SewageRequest existing = findOrThrow(id);
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
        SewageRequest saved = sewageRequestRepository.save(existing);
        notifyDecline(saved, collector, reason);
        return toResponse(saved);
    }

    public SewageRequestResponse updateStatus(String id, String collectorId, CollectionStatus newStatus) {
        SewageRequest existing = findOrThrow(id);
        if (!collectorId.equals(existing.getCollectorId())) {
            throw new CustomException("Only the assigned collector can update this request's status");
        }
        existing.setStatus(newStatus);
        SewageRequest saved = sewageRequestRepository.save(existing);
        if (newStatus == CollectionStatus.COMPLETED) {
            notifyCompletion(saved);
        }
        return toResponse(saved);
    }

    /** Client-controlled - a collector who thinks the price is too low uses decline(), not this. */
    public SewageRequestResponse updateQuote(String id, String clientId, BigDecimal quotedPrice) {
        if (quotedPrice.signum() <= 0) {
            throw new CustomException("Price must be greater than zero");
        }
        SewageRequest existing = findOrThrow(id);
        if (!clientId.equals(existing.getClientId())) {
            throw new CustomException("Only the requesting client can set the price for this request");
        }
        if (existing.getStatus() == CollectionStatus.COMPLETED || existing.getStatus() == CollectionStatus.CANCELLED) {
            throw new CustomException("Cannot update the price on a request that's already " + existing.getStatus());
        }
        existing.setQuotedPrice(quotedPrice);
        existing.setCurrency(paymentsCurrency);
        return toResponse(sewageRequestRepository.save(existing));
    }

    /** See CollectionRequestService.pay() - same speculative wallet-service contract, same stored-currency rule. */
    public SewageRequestResponse pay(String id, String clientId) {
        SewageRequest existing = findOrThrow(id);
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
        sewageRequestRepository.save(existing);

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
        return toResponse(sewageRequestRepository.save(existing));
    }

    public void cancel(String id, String clientId, String reason) {
        SewageRequest existing = findOrThrow(id);
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
        sewageRequestRepository.save(existing);
    }

    private SewageRequest findOrThrow(String id) {
        return sewageRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sewage request not found: " + id));
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

    private void notifyClientOfConfirmation(SewageRequest request) {
        userSummaryService.findById(request.getClientId()).ifPresent(client ->
                mailService.sendSewageRequestConfirmation(
                        client.getEmail(),
                        client.preferredName(),
                        request.getId(),
                        String.valueOf(request.getTankVolumeLiters()),
                        request.getUrgency().name(),
                        formatScheduledAt(request.getScheduledAt()),
                        formatLocationSummary(request.getLocation()),
                        request.getQuotedPrice(),
                        request.getCurrency()
                ));
    }

    private void notifyAssignment(SewageRequest request, UserSummary collector) {
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

    private void notifyDecline(SewageRequest request, UserSummary collector, String reason) {
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

    private void notifyCompletion(SewageRequest request) {
        // No rating feature for sewage requests yet, so no rating prompt.
        userSummaryService.findById(request.getClientId()).ifPresent(client ->
                mailService.sendRequestCompleted(
                        client.getEmail(),
                        client.preferredName(),
                        request.getId(),
                        SERVICE_TYPE,
                        false,
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

    private SewageRequestResponse toResponse(SewageRequest entity) {
        return SewageRequestResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .collectorId(entity.getCollectorId())
                .tankVolumeLiters(entity.getTankVolumeLiters())
                .urgency(entity.getUrgency())
                .accessNotes(entity.getAccessNotes())
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