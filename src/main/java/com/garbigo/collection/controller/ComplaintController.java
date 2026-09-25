package com.garbigo.collection.controller;

import com.garbigo.collection.dto.ComplaintCreateRequest;
import com.garbigo.collection.dto.ComplaintResponse;
import com.garbigo.collection.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<ComplaintResponse> create(
            Authentication authentication,
            @Valid @RequestBody ComplaintCreateRequest request) {
        ComplaintResponse response = complaintService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ComplaintResponse>> getMine(Authentication authentication) {
        return ResponseEntity.ok(complaintService.getMine(authentication.getName()));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT')")
    public ResponseEntity<ComplaintResponse> resolve(@PathVariable String id) {
        return ResponseEntity.ok(complaintService.resolve(id));
    }
}