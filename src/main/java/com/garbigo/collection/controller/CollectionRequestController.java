package com.garbigo.collection.controller;

import com.garbigo.collection.dto.CollectionRequestCreateRequest;
import com.garbigo.collection.dto.CollectionRequestResponse;
import com.garbigo.collection.dto.RatingCreateRequest;
import com.garbigo.collection.dto.RatingResponse;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.security.CallerRoles;
import com.garbigo.collection.service.CollectionRequestService;
import com.garbigo.collection.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionRequestController {

    private final CollectionRequestService collectionRequestService;
    private final RatingService ratingService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<CollectionRequestResponse> create(
            Authentication authentication,
            @Valid @RequestBody CollectionRequestCreateRequest request) {
        CollectionRequestResponse response = collectionRequestService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionRequestResponse> getById(Authentication authentication, @PathVariable String id) {
        return ResponseEntity.ok(collectionRequestService.getById(id, authentication.getName(), CallerRoles.of(authentication)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Page<CollectionRequestResponse>> getMine(
            Authentication authentication, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(collectionRequestService.getMine(authentication.getName(), pageable));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<Page<CollectionRequestResponse>> getAssigned(
            Authentication authentication, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(collectionRequestService.getAssigned(authentication.getName(), pageable));
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<List<CollectionRequestResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm) {
        return ResponseEntity.ok(collectionRequestService.findNearby(lat, lng, radiusKm));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<CollectionRequestResponse> assign(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam String collectorId) {
        return ResponseEntity.ok(collectionRequestService.assign(id, authentication.getName(), collectorId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<CollectionRequestResponse> updateStatus(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam CollectionStatus status) {
        return ResponseEntity.ok(collectionRequestService.updateStatus(id, authentication.getName(), status));
    }

    @PutMapping("/{id}/quote")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<CollectionRequestResponse> updateQuote(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam BigDecimal quotedPrice) {
        return ResponseEntity.ok(collectionRequestService.updateQuote(id, authentication.getName(), quotedPrice));
    }

    @PutMapping("/{id}/decline")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<CollectionRequestResponse> decline(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(collectionRequestService.decline(id, authentication.getName(), reason));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<CollectionRequestResponse> pay(
            Authentication authentication,
            @PathVariable String id) {
        return ResponseEntity.ok(collectionRequestService.pay(id, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> cancel(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam(required = false) String reason) {
        collectionRequestService.cancel(id, authentication.getName(), reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rating")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RatingResponse> rate(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody RatingCreateRequest request) {
        RatingResponse response = ratingService.rate(id, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}