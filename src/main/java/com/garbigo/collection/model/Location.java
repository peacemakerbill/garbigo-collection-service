package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Embedded on CollectionRequest/RecurringSchedule - not its own collection. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    private String locationName;
    private String address;
    private String landmark;
    private String city;
    private Double latitude;
    private Double longitude;
}