package com.garbigo.collection.dto;

import com.garbigo.collection.model.WasteType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * clientId/status/paymentStatus/collectorId are set by the service, not the
 * caller. Either savedLocationId or location must be present - the service
 * resolves savedLocationId if given, otherwise requires location inline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRequestCreateRequest {

    @NotNull
    private WasteType wasteType;

    @NotNull
    @Future
    private Instant scheduledAt;

    private String savedLocationId;

    @Valid
    private LocationRequest location;

    private String notes;
}