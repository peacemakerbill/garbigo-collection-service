package com.garbigo.collection.controller;

import com.garbigo.collection.dto.AdminStatsResponse;
import com.garbigo.collection.dto.CollectorPerformanceResponse;
import com.garbigo.collection.dto.ComplaintStatsResponse;
import com.garbigo.collection.dto.DailyRequestCountResponse;
import com.garbigo.collection.dto.RevenueStatsResponse;
import com.garbigo.collection.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Platform statistics. The request/complaint/user/rating/schedule management endpoints live in the other Admin*Controller classes. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminStatsService.getStats());
    }

    @GetMapping("/stats/revenue")
    public ResponseEntity<RevenueStatsResponse> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(adminStatsService.getRevenue(from, to));
    }

    @GetMapping("/stats/requests-per-day")
    public ResponseEntity<List<DailyRequestCountResponse>> requestsPerDay(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminStatsService.getRequestsPerDay(days));
    }

    @GetMapping("/stats/collectors")
    public ResponseEntity<List<CollectorPerformanceResponse>> collectors(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminStatsService.getCollectorPerformance(limit));
    }

    @GetMapping("/stats/complaints")
    public ResponseEntity<ComplaintStatsResponse> complaints() {
        return ResponseEntity.ok(adminStatsService.getComplaintStats());
    }
}