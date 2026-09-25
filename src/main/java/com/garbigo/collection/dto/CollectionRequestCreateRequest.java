package com.garbigo.collection.dto;

import com.garbigo.collection.model.WasteType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** clientId/status/paymentStatus/collectorId are set by the service, not the caller. */
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