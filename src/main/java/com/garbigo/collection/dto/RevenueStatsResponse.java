package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Totals across both collection and sewage requests that have a price, grouped by the currency each was quoted in. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatsResponse {

    /** The requested window (by request creation date); null means unbounded on that side. */
    private LocalDate from;
    private LocalDate to;

    private List<CurrencyTotals> byCurrency;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrencyTotals {

        /** "UNSPECIFIED" for a legacy record that has a price but no stored currency. */
        private String currency;

        private BigDecimal paidTotal;
        private long paidCount;

        /** UNPAID, PENDING or FAILED, excluding cancelled requests. */
        private BigDecimal outstandingTotal;
        private long outstandingCount;

        private BigDecimal refundedTotal;
        private long refundedCount;
    }
}