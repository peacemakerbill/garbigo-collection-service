package com.garbigo.collection.client;

import com.garbigo.collection.dto.WalletPaymentRequest;
import com.garbigo.collection.dto.WalletPaymentResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * garbigo-wallet-service still isn't designed - this endpoint path and the
 * request/response shape are a guess, mirroring the /internal/* convention
 * auth-service established for its own internal-key-protected routes, not a
 * confirmed contract. Update this once wallet-service's real API exists.
 */
@FeignClient(name = "wallet-service", url = "${services.wallet.base-url}")
public interface WalletServiceClient {

    @PostMapping("/internal/payments")
    WalletPaymentResult initiatePayment(@RequestBody WalletPaymentRequest request);
}