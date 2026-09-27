package com.garbigo.collection.controller;

import com.garbigo.collection.dto.SavedLocationCreateRequest;
import com.garbigo.collection.dto.SavedLocationResponse;
import com.garbigo.collection.service.SavedLocationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/saved-locations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class SavedLocationController {

    private final SavedLocationService savedLocationService;

    @PostMapping
    public ResponseEntity<SavedLocationResponse> create(
            Authentication authentication,
            @Valid @RequestBody SavedLocationCreateRequest request) {
        SavedLocationResponse response = savedLocationService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<SavedLocationResponse>> getMine(Authentication authentication) {
        return ResponseEntity.ok(savedLocationService.getMine(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable String id) {
        savedLocationService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}