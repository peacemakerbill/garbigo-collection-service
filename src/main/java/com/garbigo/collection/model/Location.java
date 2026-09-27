package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;

/**
 * Embedded on CollectionRequest/SewageRequest/RecurringSchedule/
 * SavedLocation - not its own collection.
 *
 * coordinates mirrors latitude/longitude as a GeoJsonPoint so the parent
 * document's 2dsphere index (see CollectionRequest/SewageRequest) can
 * support proximity queries ("nearby requests" for a collector) - populate
 * both together (LocationMapper does this), latitude/longitude stay the
 * user-facing fields in the API.
 */
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

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint coordinates;
}