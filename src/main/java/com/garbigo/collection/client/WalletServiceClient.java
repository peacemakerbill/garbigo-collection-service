package com.garbigo.collection.client;

import org.springframework.cloud.openfeign.FeignClient;

/** Empty until garbigo-wallet-service's own API is designed. */
@FeignClient(name = "wallet-service", url = "${services.wallet.base-url}")
public interface WalletServiceClient {
}