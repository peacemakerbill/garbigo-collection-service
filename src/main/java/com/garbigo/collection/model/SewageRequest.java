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
 * Sewage/exhauster pickup request - a separate document from
 * CollectionRequest since the fields that actually matter here (tank
 * volume, urgency, truck access) don't overlap much with garbage pickups,
 * but reuses CollectionStatus/PaymentStatus since the lifecycle is the
 * same. Whether a COLLECTOR can service both domains or needs a distinct
 * role/service-type on the auth-service side isn't settled - assumed here
 * to be the same COLLECTOR role as CollectionRequest.
 */
@Document(collection = "sewage_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SewageRequest {

    @Id
    private String id;

    private String clientId;
    private String collectorId;
    private Integer tankVolumeLiters;
    private SewageUrgency urgency;
    private String accessNotes;
    private CollectionStatus status;
    private PaymentStatus paymentStatus;
    private Instant scheduledAt;
    private Location location;
    private String notes;
    private BigDecimal quotedPrice;
    private String cancellationReason;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}