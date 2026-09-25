package com.garbigo.collection.controller;

import com.garbigo.collection.dto.ScheduleCreateRequest;
import com.garbigo.collection.dto.ScheduleResponse;
import com.garbigo.collection.service.SchedulingService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ScheduleController {

    private final SchedulingService schedulingService;

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            Authentication authentication,
            @Valid @RequestBody ScheduleCreateRequest request) {
        ScheduleResponse response = schedulingService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ScheduleResponse>> getMine(Authentication authentication) {
        return ResponseEntity.ok(schedulingService.getMine(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody ScheduleCreateRequest request) {
        return ResponseEntity.ok(schedulingService.update(id, authentication.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable String id) {
        schedulingService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}