package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed pickup/drop-off location, embedded on {@link CollectionRequest}
 * and {@link RecurringSchedule} - not its own top-level Mongo collection or
 * controller, since a location only ever exists attached to a request or
 * schedule.
 *
 * <p>This is distinct from auth-service's live-location tracking: that's a
 * user's current position; this is the fixed pickup point for a job, used
 * so a client or collector's app can show it on a map and get directions.
 *
 * <p>{@code latitude}/{@code longitude} are plain doubles for now. If
 * "find requests near me"-style proximity queries become a real need, this
 * is the natural place to switch to a MongoDB {@code GeoJsonPoint} with a
 * {@code 2dsphere} index - not done yet since nothing here queries by
 * proximity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    /** Human label, e.g. "Home", "Office - Westlands". */
    private String locationName;

    /** Detailed street/building address. */
    private String address;

    /** Optional landmark, e.g. "next to Total petrol station". */
    private String landmark;

    private String city;

    private Double latitude;

    private Double longitude;
}
