package com.garbigo.collection.messaging;

import org.springframework.stereotype.Component;

/**
 * Future consumer of a payment-confirmed event from garbigo-wallet-service,
 * which would flip a CollectionRequest's paymentStatus to PAID (or FAILED).
 *
 * <p>Deliberately inert for now: wallet-service doesn't exist yet, so
 * there's no queue name, exchange, or event payload shape to bind to. Do
 * not add a {@code @RabbitListener} annotation here until wallet-service's
 * actual contract is designed - guessing a queue name now would either
 * silently bind to nothing or fail startup if RabbitMQ can't find it.
 *
 * <p>TODO: once wallet-service exists, this should look like
 * {@link UserCreatedEventListener} - a {@code @RabbitListener(queues = ...)}
 * method that looks up the CollectionRequest by id (via
 * CollectionRequestRepository, injected once this class actually does
 * something) and sets paymentStatus to PAID or FAILED based on the event.
 */
@Component
public class PaymentConfirmedEventListener {
}
