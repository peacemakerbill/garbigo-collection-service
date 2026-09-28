package com.garbigo.collection.controller;

import com.garbigo.collection.dto.CollectorResponse;
import com.garbigo.collection.service.CollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/collectors")
@RequiredArgsConstructor
public class CollectorController {

    private final CollectorService collectorService;

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<CollectorResponse>> list() {
        return ResponseEntity.ok(collectorService.listActiveCollectors());
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<CollectorResponse>> nearby(
            @RequestHeader("Authorization") String authorization,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm) {
        return ResponseEntity.ok(collectorService.listNearbyCollectors(authorization, lat, lng, radiusKm));
    }
}