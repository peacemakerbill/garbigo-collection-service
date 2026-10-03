package com.garbigo.collection.dto;

import com.garbigo.collection.model.SewageUrgency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Same rules as CollectionRequestUpdateRequest: every field optional, null means unchanged. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SewageRequestUpdateRequest {

    @Positive
    private Integer tankVolumeLiters;

    private SewageUrgency urgency;

    @Future
    private Instant scheduledAt;

    private String savedLocationId;

    @Valid
    private LocationRequest location;

    private String accessNotes;
    private String notes;
}