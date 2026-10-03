package com.garbigo.collection.dto;

import com.garbigo.collection.model.WasteType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Every field is optional and null means "leave as it is" (send an empty string
 * for notes to clear them). Provide savedLocationId or location to move the
 * pickup, not both.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRequestUpdateRequest {

    private WasteType wasteType;

    @Future
    private Instant scheduledAt;

    private String savedLocationId;

    @Valid
    private LocationRequest location;

    private String notes;
}