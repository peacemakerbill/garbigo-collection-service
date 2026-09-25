package com.garbigo.collection.controller;

import com.garbigo.collection.dto.CollectionRequestCreateRequest;
import com.garbigo.collection.dto.CollectionRequestResponse;
import com.garbigo.collection.model.CollectionStatus;
import com.garbigo.collection.service.CollectionRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionRequestController {

    private final CollectionRequestService collectionRequestService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<CollectionRequestResponse> create(
            Authentication authentication,
            @Valid @RequestBody CollectionRequestCreateRequest request) {
        CollectionRequestResponse response = collectionRequestService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionRequestResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(collectionRequestService.getById(id));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<CollectionRequestResponse>> getMine(Authentication authentication) {
        return ResponseEntity.ok(collectionRequestService.getMine(authentication.getName()));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<List<CollectionRequestResponse>> getAssigned(Authentication authentication) {
        return ResponseEntity.ok(collectionRequestService.getAssigned(authentication.getName()));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS')")
    public ResponseEntity<CollectionRequestResponse> assign(
            @PathVariable String id,
            @RequestParam String collectorId) {
        return ResponseEntity.ok(collectionRequestService.assign(id, collectorId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<CollectionRequestResponse> updateStatus(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam CollectionStatus status) {
        return ResponseEntity.ok(collectionRequestService.updateStatus(id, authentication.getName(), status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable String id) {
        collectionRequestService.cancel(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}