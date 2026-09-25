package com.garbigo.collection.dto;

import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.PaymentStatus;
import com.garbigo.collection.model.WasteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRequestResponse {

    private String id;
    private String clientId;
    private String collectorId;
    private WasteType wasteType;
    private CollectionStatus status;
    private PaymentStatus paymentStatus;
    private Instant scheduledAt;
    private LocationResponse location;
    private String notes;
    private BigDecimal quotedPrice;
    private Instant createdAt;
    private Instant updatedAt;
}
