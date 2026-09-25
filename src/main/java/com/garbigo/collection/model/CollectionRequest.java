package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single pickup request/booking created by a client.
 *
 * <p>{@code clientId} and {@code collectorId} reference auth-service users
 * by id only - this service never stores their profile data beyond what's
 * cached in {@link UserSummary}.
 *
 * <p>TODO: scaffolding placeholder - no validation, MongoDB indexes (e.g. a
 * compound index on clientId + status), or business rules have been added
 * yet.
 */
@Document(collection = "collection_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRequest {

    @Id
    private String id;

    private String clientId;

    private String collectorId;

    private WasteType wasteType;

    private CollectionStatus status;

    private PaymentStatus paymentStatus;

    private Instant scheduledAt;

    private Location location;

    private String notes;

    private BigDecimal quotedPrice;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
