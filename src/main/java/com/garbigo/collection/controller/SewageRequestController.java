package com.garbigo.collection.controller;

import com.garbigo.collection.dto.SewageRequestCreateRequest;
import com.garbigo.collection.dto.SewageRequestResponse;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.service.SewageRequestService;
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

@RestController
@RequestMapping("/api/v1/sewage-requests")
@RequiredArgsConstructor
public class SewageRequestController {

    private final SewageRequestService sewageRequestService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<SewageRequestResponse> create(
            Authentication authentication,
            @Valid @RequestBody SewageRequestCreateRequest request) {
        SewageRequestResponse response = sewageRequestService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SewageRequestResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(sewageRequestService.getById(id));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Page<SewageRequestResponse>> getMine(
            Authentication authentication, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(sewageRequestService.getMine(authentication.getName(), pageable));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<Page<SewageRequestResponse>> getAssigned(
            Authentication authentication, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(sewageRequestService.getAssigned(authentication.getName(), pageable));
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