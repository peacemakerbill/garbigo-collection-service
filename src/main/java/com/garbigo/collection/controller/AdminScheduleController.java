package com.garbigo.collection.controller;

import com.garbigo.collection.dto.ScheduleResponse;
import com.garbigo.collection.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin view of every client's recurring schedules - pause or resume them, or remove one outright. */
@RestController
@RequestMapping("/api/v1/admin/schedules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminScheduleController {

    private final SchedulingService schedulingService;

    @GetMapping
    public ResponseEntity<Page<ScheduleResponse>> search(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(schedulingService.adminSearch(clientId, active, pageable));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<ScheduleResponse> setActive(
            Authentication authentication, @PathVariable String id, @RequestParam boolean active) {
        return ResponseEntity.ok(schedulingService.adminSetActive(authentication.getName(), id, active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable String id) {
        schedulingService.adminDelete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}