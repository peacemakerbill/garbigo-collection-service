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

import java.time.Instant;

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
}