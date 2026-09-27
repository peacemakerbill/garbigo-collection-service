package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponse {

    private BigDecimal estimatedPrice;
    private String currency;
    private String disclaimer;
}