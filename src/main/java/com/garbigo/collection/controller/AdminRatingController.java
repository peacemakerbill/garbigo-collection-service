package com.garbigo.collection.controller;

import com.garbigo.collection.dto.AdminRatingResponse;
import com.garbigo.collection.service.AdminRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ratings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRatingController {

    private final AdminRatingService adminRatingService;

    /** maxScore=2, for example, lists only 1-2 star ratings. */
    @GetMapping
    public ResponseEntity<Page<AdminRatingResponse>> search(
            @RequestParam(required = false) String collectorId,
            @RequestParam(required = false) Integer maxScore,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminRatingService.search(collectorId, maxScore, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable String id) {
        adminRatingService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}