package com.garbigo.collection.model;

/**
 * Payment lifecycle for a {@link CollectionRequest}, driven by
 * garbigo-wallet-service (not yet designed) via
 * {@link com.garbigo.collection.messaging.PaymentConfirmedEventListener} -
 * this service never processes payments itself. Kept as a field separate
 * from {@link CollectionStatus} since job progress and payment progress are
 * different concerns owned by different services; whether e.g. COMPLETED
 * should require PAID is a business rule to decide later, not baked in
 * here.
 */
public enum PaymentStatus {
    UNPAID,
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
