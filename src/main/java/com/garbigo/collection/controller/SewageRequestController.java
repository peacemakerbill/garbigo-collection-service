package com.garbigo.collection.controller;

import com.garbigo.collection.dto.MyRequestFilter;
import com.garbigo.collection.dto.RatingCreateRequest;
import com.garbigo.collection.dto.RatingResponse;
import com.garbigo.collection.dto.SewageRequestCreateRequest;
import com.garbigo.collection.dto.SewageRequestResponse;
import com.garbigo.collection.dto.SewageRequestUpdateRequest;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.security.CallerRoles;
import com.garbigo.collection.service.RatingService;
import com.garbigo.collection.service.SewageRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sewage-requests")
@RequiredArgsConstructor
public class SewageRequestController {

    private final SewageRequestService sewageRequestService;
    private final RatingService ratingService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<SewageRequestResponse> create(
            Authentication authentication,
            @Valid @RequestBody SewageRequestCreateRequest request) {
        SewageRequestResponse response = sewageRequestService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SewageRequestResponse> getById(Authentication authentication, @PathVariable String id) {
        return ResponseEntity.ok(sewageRequestService.getById(id, authentication.getName(), CallerRoles.of(authentication)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Page<SewageRequestResponse>> getMine(
            Authentication authentication,
            @RequestParam(required = false) CollectionStatus status,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Instant scheduledFrom,
            @RequestParam(required = false) Instant scheduledTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        MyRequestFilter filter = MyRequestFilter.builder()
                .status(status).active(active).scheduledFrom(scheduledFrom).scheduledTo(scheduledTo).build();
        return ResponseEntity.ok(sewageRequestService.getMine(authentication.getName(), filter, pageable));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<Page<SewageRequestResponse>> getAssigned(
            Authentication authentication,
            @RequestParam(required = false) CollectionStatus status,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Instant scheduledFrom,
            @RequestParam(required = false) Instant scheduledTo,
            @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.ASC) Pageable pageable) {
        MyRequestFilter filter = MyRequestFilter.builder()
                .status(status).active(active).scheduledFrom(scheduledFrom).scheduledTo(scheduledTo).build();
        return ResponseEntity.ok(sewageRequestService.getAssigned(authentication.getName(), filter, pageable));
    }

    /** Pending sewage requests near a point, nearest first - the sewage twin of GET /collections/nearby. */
    @GetMapping("/nearby")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<List<SewageRequestResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm) {
        return ResponseEntity.ok(sewageRequestService.findNearby(lat, lng, radiusKm));
    }

    @PostMapping("/{id}/rating")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<RatingResponse> rate(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody RatingCreateRequest request) {
        RatingResponse response = ratingService.rateSewage(id, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<SewageRequestResponse> update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody SewageRequestUpdateRequest request) {
        return ResponseEntity.ok(sewageRequestService.update(id, authentication.getName(), request));
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<SewageRequestResponse> accept(Authentication authentication, @PathVariable String id) {
        return ResponseEntity.ok(sewageRequestService.accept(id, authentication.getName()));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<SewageRequestResponse> confirm(Authentication authentication, @PathVariable String id) {
        return ResponseEntity.ok(sewageRequestService.confirm(id, authentication.getName()));
    }

    /** reason is optional here only so a missing one gets the service's clear 400 rather than a bare MVC error. */
    @PutMapping("/{id}/dispute")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<SewageRequestResponse> dispute(
            Authentication authentication, @PathVariable String id, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(sewageRequestService.dispute(id, authentication.getName(), reason));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<SewageRequestResponse> assign(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam String collectorId) {
        return ResponseEntity.ok(sewageRequestService.assign(id, authentication.getName(), collectorId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<SewageRequestResponse> updateStatus(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam CollectionStatus status) {
        return ResponseEntity.ok(sewageRequestService.updateStatus(id, authentication.getName(), status));
    }

    @PutMapping("/{id}/quote")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<SewageRequestResponse> updateQuote(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam BigDecimal quotedPrice) {
        return ResponseEntity.ok(sewageRequestService.updateQuote(id, authentication.getName(), quotedPrice));
    }

    @PutMapping("/{id}/decline")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<SewageRequestResponse> decline(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(sewageRequestService.decline(id, authentication.getName(), reason));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<SewageRequestResponse> pay(
            Authentication authentication,
            @PathVariable String id) {
        return ResponseEntity.ok(sewageRequestService.pay(id, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> cancel(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam(required = false) String reason) {
        sewageRequestService.cancel(id, authentication.getName(), reason);
        return ResponseEntity.noContent().build();
    }
}