package com.garbigo.collection.dto;

import com.garbigo.collection.model.WasteType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * clientId/status/paymentStatus/collectorId are set by the service, not the
 * caller. Either savedLocationId or location must be present - the service
 * resolves savedLocationId if given, otherwise requires location inline.
 *
 * quotedPrice here is what the client is proposing to pay - optional, and
 * not the only way a price gets set: the assigned collector can also set or
 * adjust it later via PUT /{id}/quote. Neither side is required to guess at
 * a system-generated estimate.
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

    @Positive
    private BigDecimal quotedPrice;
}