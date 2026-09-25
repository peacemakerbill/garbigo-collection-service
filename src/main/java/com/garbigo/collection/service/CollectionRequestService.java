package com.garbigo.collection.service;

import com.garbigo.collection.dto.CollectionRequestCreateRequest;
import com.garbigo.collection.dto.CollectionRequestResponse;
import com.garbigo.collection.dto.LocationRequest;
import com.garbigo.collection.dto.LocationResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.model.CollectionRequest;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.Location;
import com.garbigo.collection.model.PaymentStatus;
import com.garbigo.collection.notification.MailService;
import com.garbigo.collection.repository.CollectionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionRequestService {

    private static final DateTimeFormatter SCHEDULED_AT_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    private final CollectionRequestRepository collectionRequestRepository;
    private final UserSummaryService userSummaryService;
    private final MailService mailService;

    public CollectionRequestResponse create(String clientId, CollectionRequestCreateRequest request) {
        CollectionRequest saved = collectionRequestRepository.save(
                CollectionRequest.builder()
                        .clientId(clientId)
                        .wasteType(request.getWasteType())
                        .status(CollectionStatus.PENDING)
                        .paymentStatus(PaymentStatus.UNPAID)
                        .scheduledAt(request.getScheduledAt())
                        .location(toLocation(request.getLocation()))
                        .notes(request.getNotes())
                        .build()
        );
        notifyClientOfConfirmation(saved);
        return toResponse(saved);
    }

    public CollectionRequestResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    public List<CollectionRequestResponse> getMine(String clientId) {
        return collectionRequestRepository.findByClientId(clientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CollectionRequestResponse> getAssigned(String collectorId) {
        return collectionRequestRepository.findByCollectorId(collectorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CollectionRequestResponse assign(String id, String collectorId) {
        CollectionRequest existing = findOrThrow(id);
        existing.setCollectorId(collectorId);
        existing.setStatus(CollectionStatus.ASSIGNED);
        return toResponse(collectionRequestRepository.save(existing));
    }

    public CollectionRequestResponse updateStatus(String id, String collectorId, CollectionStatus newStatus) {
        CollectionRequest existing = findOrThrow(id);
        if (!collectorId.equals(existing.getCollectorId())) {
            throw new CustomException("Only the assigned collector can update this request's status");
        }
        existing.setStatus(newStatus);
        return toResponse(collectionRequestRepository.save(existing));
    }

    public void cancel(String id, String clientId) {
        CollectionRequest existing = findOrThrow(id);
        if (!clientId.equals(existing.getClientId())) {
            throw new CustomException("Only the requesting client can cancel this request");
        }
        existing.setStatus(CollectionStatus.CANCELLED);
        collectionRequestRepository.save(existing);
    }

    private CollectionRequest findOrThrow(String id) {
        return collectionRequestRepository.findById(id)
                .orElseThrow(() -> new CustomException("Collection request not found: " + id));
    }

    private void notifyClientOfConfirmation(CollectionRequest request) {
        userSummaryService.findById(request.getClientId()).ifPresent(client -> {
            String scheduledAt = request.getScheduledAt() == null
                    ? "TBD"
                    : SCHEDULED_AT_FORMAT.format(request.getScheduledAt().atZone(ZoneOffset.UTC));
            String locationSummary = request.getLocation() == null
                    ? "TBD"
                    : String.join(", ",
                            request.getLocation().getLocationName(),
                            request.getLocation().getAddress());

            mailService.sendCollectionRequestConfirmation(
                    client.getEmail(),
                    client.getDisplayUsername(),
                    request.getId(),
                    request.getWasteType().name(),
                    scheduledAt,
                    locationSummary
            );
        });
    }

    private Location toLocation(LocationRequest request) {
        return Location.builder()
                .locationName(request.getLocationName())
                .address(request.getAddress())
                .landmark(request.getLandmark())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
    }

    private LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }
        return LocationResponse.builder()
                .locationName(location.getLocationName())
                .address(location.getAddress())
                .landmark(location.getLandmark())
                .city(location.getCity())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
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
                .location(toLocationResponse(entity.getLocation()))
                .notes(entity.getNotes())
                .quotedPrice(entity.getQuotedPrice())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}