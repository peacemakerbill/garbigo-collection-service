package com.garbigo.collection.model;

/** Job status - separate from {@link PaymentStatus}. */
public enum CollectionStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    DISPUTED
}