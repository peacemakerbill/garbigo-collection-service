package com.garbigo.collection.service;

import com.garbigo.collection.dto.CollectionQuoteRequest;
import com.garbigo.collection.dto.QuoteResponse;
import com.garbigo.collection.dto.SewageQuoteRequest;
import com.garbigo.collection.model.SewageUrgency;
import com.garbigo.collection.model.WasteType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Flat heuristic pricing, not a real pricing engine - gives a client a
 * rough number before booking. Base prices are placeholders; swap for
 * whatever the business actually charges once that's decided.
 */
@Service
public class PricingService {

    private static final String CURRENCY = "KES";
    private static final String DISCLAIMER = "This is an estimate. Final price is confirmed by your collector.";

    private static final Map<WasteType, BigDecimal> WASTE_BASE_PRICES = Map.of(
            WasteType.GENERAL, new BigDecimal("500"),
            WasteType.ORGANIC, new BigDecimal("400"),
            WasteType.RECYCLABLE, new BigDecimal("300"),
            WasteType.HAZARDOUS, new BigDecimal("1500"),
            WasteType.ELECTRONIC, new BigDecimal("800"),
            WasteType.BULK, new BigDecimal("1200")
    );

    private static final BigDecimal SEWAGE_PRICE_PER_LITER = new BigDecimal("2.5");
    private static final BigDecimal SEWAGE_URGENT_SURCHARGE = new BigDecimal("1000");

    public QuoteResponse quoteCollection(CollectionQuoteRequest request) {
        BigDecimal price = WASTE_BASE_PRICES.getOrDefault(request.getWasteType(), new BigDecimal("500"));
        return QuoteResponse.builder()
                .estimatedPrice(price)
                .currency(CURRENCY)
                .disclaimer(DISCLAIMER)
                .build();
    }

    public QuoteResponse quoteSewage(SewageQuoteRequest request) {
        BigDecimal price = SEWAGE_PRICE_PER_LITER.multiply(BigDecimal.valueOf(request.getTankVolumeLiters()));
        if (request.getUrgency() == SewageUrgency.URGENT) {
            price = price.add(SEWAGE_URGENT_SURCHARGE);
        }
        return QuoteResponse.builder()
                .estimatedPrice(price)
                .currency(CURRENCY)
                .disclaimer(DISCLAIMER)
                .build();
    }
}