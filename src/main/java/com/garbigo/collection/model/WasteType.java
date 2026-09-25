package com.garbigo.collection.model;

/**
 * The category of waste being collected, on a {@link CollectionRequest} or
 * declared upfront on a {@link RecurringSchedule}.
 */
public enum WasteType {
    GENERAL,
    ORGANIC,
    RECYCLABLE,
    HAZARDOUS,
    ELECTRONIC,
    BULK
}
