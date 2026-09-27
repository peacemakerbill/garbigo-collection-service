package com.garbigo.collection.controller;

import com.garbigo.collection.dto.CollectionQuoteRequest;
import com.garbigo.collection.dto.QuoteResponse;
import com.garbigo.collection.dto.SewageQuoteRequest;
import com.garbigo.collection.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** No auth requirement - a prospective client should be able to check pricing before signing up. */
@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final PricingService pricingService;

    @PostMapping("/collections")
    public ResponseEntity<QuoteResponse> quoteCollection(@Valid @RequestBody CollectionQuoteRequest request) {
        return ResponseEntity.ok(pricingService.quoteCollection(request));
    }

    @PostMapping("/sewage")
    public ResponseEntity<QuoteResponse> quoteSewage(@Valid @RequestBody SewageQuoteRequest request) {
        return ResponseEntity.ok(pricingService.quoteSewage(request));
    }
}