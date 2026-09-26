package com.garbigo.collection.client;

import com.garbigo.collection.model.UserSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Confirmed against auth-service: GET /internal/users, gated by
 * hasRole("INTERNAL") via the X-Internal-Api-Key header (attached globally
 * by FeignConfig) - a user's own JWT does not work here. Unpaginated by
 * design (returns the whole collection every call), so UserSummaryService
 * treats it as a full directory refresh, not a per-user lookup - there's no
 * confirmed single-user internal endpoint, so we don't guess one.
 */
@FeignClient(name = "auth-service", url = "${services.auth.base-url}")
public interface AuthServiceClient {

    @GetMapping("/internal/users")
    List<UserSummary> getAllUsers();
}