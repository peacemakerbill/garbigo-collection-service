package com.garbigo.collection.controller;

import com.garbigo.collection.dto.AdminUserDetailResponse;
import com.garbigo.collection.dto.AdminUserResponse;
import com.garbigo.collection.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only: users belong to auth-service, so creating, editing, deactivating
 * or changing a role happens there. This shows the local copy plus their
 * activity in this service.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /** q matches a substring of email, phone number, username or any name part. */
    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> search(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "email") Pageable pageable) {
        return ResponseEntity.ok(adminUserService.search(role, active, archived, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDetailResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(adminUserService.get(id));
    }
}