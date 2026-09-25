package com.garbigo.collection.client;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * Seam for future calls into garbigo-wallet-service, which doesn't exist
 * yet. Left with no methods deliberately - filling this in before
 * wallet-service's own API is designed would just be guessing at someone
 * else's contract.
 *
 * <p>Likely candidate once that service exists: something like
 * {@code POST /payments/confirm} to request payment for a
 * CollectionRequest, mirroring how {@link AuthServiceClient} calls
 * auth-service. The reverse direction - wallet-service telling this service
 * a payment completed - is handled separately, over RabbitMQ, by
 * {@link com.garbigo.collection.messaging.PaymentConfirmedEventListener}.
 */
@FeignClient(name = "wallet-service", url = "${garbigo.wallet-service.base-url}")
public interface WalletServiceClient {
}
