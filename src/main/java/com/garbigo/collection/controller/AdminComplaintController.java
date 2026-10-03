package com.garbigo.collection.controller;

import com.garbigo.collection.dto.ComplaintResponse;
import com.garbigo.collection.model.ComplaintCategory;
import com.garbigo.collection.model.ComplaintStatus;
import com.garbigo.collection.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin view of every complaint. No hard delete: a complaint is a record of
 * what was reported, so the way to close one out is a status, not removal.
 * (PUT /api/v1/complaints/{id}/resolve, the original resolve endpoint, still works.)
 */
@RestController
@RequestMapping("/api/v1/admin/complaints")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<Page<ComplaintResponse>> search(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) String reporterId,
            @RequestParam(required = false) String collectionRequestId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(complaintService.adminSearch(status, category, reporterId, collectionRequestId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(complaintService.adminGet(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            Authentication authentication, @PathVariable String id, @RequestParam ComplaintStatus status) {
        return ResponseEntity.ok(complaintService.adminSetStatus(authentication.getName(), id, status));
    }
}