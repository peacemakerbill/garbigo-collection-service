package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Best-effort guess at garbigo-wallet-service's payment-initiation contract
 * - that service isn't designed yet (unlike auth-service's confirmed
 * /internal/users), so this shape, WalletServiceClient's endpoint path, and
 * WalletPaymentResult below are all speculative. Confirm and adjust once
 * wallet-service actually exists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPaymentRequest {

    private String payerId;
    private String payeeId;
    private String referenceId;
    private BigDecimal amount;
    private String currency;
}