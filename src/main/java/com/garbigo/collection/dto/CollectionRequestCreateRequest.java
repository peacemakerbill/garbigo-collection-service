package com.garbigo.collection.dto;

import com.garbigo.collection.model.WasteType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Body for POST /collections. clientId/collectorId/status/paymentStatus are
 * deliberately absent here - they're set by the service layer, not the
 * caller (clientId from the authenticated principal, status defaults to
 * PENDING, paymentStatus to UNPAID, collectorId assigned later via
 * PUT /collections/{id}/assign).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRequestCreateRequest {

    @NotNull
    private WasteType wasteType;

    @NotNull
    private Instant scheduledAt;

    @Valid
    @NotNull
    private LocationRequest location;

    private String notes;
}
