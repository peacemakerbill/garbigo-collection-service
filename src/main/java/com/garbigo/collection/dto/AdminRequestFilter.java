package com.garbigo.collection.dto;

import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.model.PaymentStatus;
import com.garbigo.collection.model.SewageUrgency;
import com.garbigo.collection.model.WasteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Optional filters for the admin request listings; any left null is simply not applied. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRequestFilter {

    private CollectionStatus status;
    private PaymentStatus paymentStatus;
    private String clientId;
    private String collectorId;

    /** Collection requests only - ignored for sewage. */
    private WasteType wasteType;

    /** Sewage requests only - ignored for collections. */
    private SewageUrgency urgency;

    private Instant createdFrom;
    private Instant createdTo;
}