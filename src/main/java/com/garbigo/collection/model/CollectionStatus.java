package com.garbigo.collection.model;

/**
 * Lifecycle status of a {@link CollectionRequest}'s underlying job (who's
 * doing it and how far along it is). Deliberately separate from
 * {@link PaymentStatus}, which tracks money instead.
 */
public enum CollectionStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    DISPUTED
}
