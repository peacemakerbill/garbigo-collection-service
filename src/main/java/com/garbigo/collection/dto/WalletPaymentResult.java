package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** See WalletPaymentRequest - speculative, wallet-service isn't designed yet. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPaymentResult {

    private boolean success;
    private String transactionId;
    private String message;
}