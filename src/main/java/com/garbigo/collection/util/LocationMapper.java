package com.garbigo.collection.util;

import com.garbigo.collection.dto.LocationRequest;
import com.garbigo.collection.dto.LocationResponse;
import com.garbigo.collection.model.Location;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

/** Shared by every service that embeds a Location (collections, sewage requests, schedules, saved locations). */
public final class LocationMapper {

    private LocationMapper() {
    }

    public static Location toLocation(LocationRequest request) {
        if (request == null) {
            return null;
        }
        return Location.builder()
                .locationName(request.getLocationName())
                .address(request.getAddress())
                .landmark(request.getLandmark())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                // GeoJsonPoint takes (x=longitude, y=latitude), not (lat, lng).
                .coordinates(new GeoJsonPoint(request.getLongitude(), request.getLatitude()))
                .build();
    }

    public static LocationResponse toResponse(Location location) {
        if (location == null) {
            return null;
        }
        return LocationResponse.builder()
                .locationName(location.getLocationName())
                .address(location.getAddress())
                .landmark(location.getLandmark())
                .city(location.getCity())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }
}