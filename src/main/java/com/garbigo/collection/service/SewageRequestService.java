package com.garbigo.collection.service;

import com.garbigo.collection.dto.SewageRequestCreateRequest;
import com.garbigo.collection.dto.SewageRequestResponse;
import com.garbigo.collection.exception.CustomException;
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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;

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

    @Value("${collections.cancellation-cutoff-hours}")
    private long cancellationCutoffHours;

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
                        .build()
        );
        notifyClientOfConfirmation(saved);
        return toResponse(saved);
    }

    public SewageRequestResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    public Page<SewageRequestResponse> getMine(String clientId, Pageable pageable) {
        return sewageRequestRepository.findByClientId(clientId, pageable).map(this::toResponse);
    }

    public Page<SewageRequestResponse> getAssigned(String collectorId, Pageable pageable) {
        return sewageRequestRepository.findByCollectorId(collectorId, pageable).map(this::toResponse);
    }

    public SewageRequestResponse assign(String id, String clientId, String collectorId) {
        SewageRequest existing = findOrThrow(id);
        if (!clientId.equals(existing.getClientId())) {
            throw new CustomException("Only the requesting client can assign a collector to this request");
        }

        UserSummary collector = userSummaryService.resolve(collectorId)
                .filter(user -> COLLECTOR_ROLE.equalsIgnoreCase(user.getRole()))
                .orElseThrow(() -> new CustomException("The selected user is not a collector: " + collectorId));

        existing.setCollectorId(collector.getId());
        existing.setStatus(CollectionStatus.ASSIGNED);
        SewageRequest saved = sewageRequestRepository.save(existing);
        notifyAssignment(saved, collector);
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
                .orElseThrow(() -> new CustomException("Sewage request not found: " + id));
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
                        formatLocationSummary(request.getLocation())
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
                        locationSummary
                ));

        mailService.sendJobAssignedToCollector(
                collector.getEmail(),
                collector.preferredName(),
                request.getId(),
                SERVICE_TYPE,
                scheduledAt,
                locationSummary
        );
    }

    private void notifyCompletion(SewageRequest request) {
        // No rating feature for sewage requests yet, so no rating prompt.
        userSummaryService.findById(request.getClientId()).ifPresent(client ->
                mailService.sendRequestCompleted(
                        client.getEmail(),
                        client.preferredName(),
                        request.getId(),
                        SERVICE_TYPE,
                        false
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
                .cancellationReason(entity.getCancellationReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}