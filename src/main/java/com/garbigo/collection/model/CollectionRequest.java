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

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}