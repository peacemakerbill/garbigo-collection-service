package com.garbigo.collection.dto;

import com.garbigo.collection.model.SewageUrgency;
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
 * quotedPrice here is what the client is proposing to pay - optional, and
 * not the only way a price gets set: the assigned collector can also set or
 * adjust it later via PUT /{id}/quote.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SewageRequestCreateRequest {

    @NotNull
    @Positive
    private Integer tankVolumeLiters;

    @NotNull
    private SewageUrgency urgency;

    @NotNull
    @Future
    private Instant scheduledAt;

    private String savedLocationId;

    @Valid
    private LocationRequest location;

    private String accessNotes;
    private String notes;

    @Positive
    private BigDecimal quotedPrice;
}