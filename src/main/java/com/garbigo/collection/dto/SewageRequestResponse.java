package com.garbigo.collection.dto;

import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.PaymentStatus;
import com.garbigo.collection.model.SewageUrgency;
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
public class SewageRequestResponse {

    private String id;
    private String clientId;
    private String collectorId;
    private Integer tankVolumeLiters;
    private SewageUrgency urgency;
    private String accessNotes;
    private CollectionStatus status;
    private PaymentStatus paymentStatus;
    private Instant scheduledAt;
    private LocationResponse location;
    private String notes;
    private BigDecimal quotedPrice;
    private String cancellationReason;
    private String lastDeclineReason;
    private String currency;
    private Instant createdAt;
    private Instant updatedAt;
}