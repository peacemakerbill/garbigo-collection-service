package com.garbigo.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Location payload embedded in create/update requests for a
 * CollectionRequest or RecurringSchedule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequest {

    @NotBlank
    private String locationName;

    @NotBlank
    private String address;

    private String landmark;

    private String city;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;
}
