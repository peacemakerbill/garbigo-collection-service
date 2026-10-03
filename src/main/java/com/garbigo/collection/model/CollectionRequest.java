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
    private String cancellationReason;
    private String lastDeclineReason;
    private String currency;

    /** When the client confirmed the completed job was done; once set, it can no longer be disputed. */
    private Instant confirmedAt;

    /** Why the client disputed a completed job (status DISPUTED). */
    private String disputeReason;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}