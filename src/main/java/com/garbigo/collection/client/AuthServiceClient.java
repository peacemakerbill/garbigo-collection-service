package com.garbigo.collection.client;

import com.garbigo.collection.dto.LiveLocationResponse;
import com.garbigo.collection.model.UserSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * getAllUsers() is confirmed against auth-service: GET /internal/users,
 * gated by hasRole("INTERNAL") via the X-Internal-Api-Key header (attached
 * globally by FeignConfig) - a user's own JWT does not work here.
 * Unpaginated by design, so UserSummaryService treats it as a full
 * directory refresh, not a per-user lookup.
 *
 * getLiveLocation() is NOT confirmed the same way - auth-service's own
 * Postman collection lists GET /users/live-location/{id} under a plain
 * "location" folder, separate from the internal-key-protected routes, so
 * it may actually be JWT-only rather than internal-key-accessible. If calls
 * to it start failing with 401/403, that's why - this needs auth-service's
 * real access rules confirmed.
 */
@FeignClient(name = "auth-service", url = "${services.auth.base-url}")
public interface AuthServiceClient {

    @GetMapping("/internal/users")
    List<UserSummary> getAllUsers();

    @GetMapping("/users/live-location/{id}")
    LiveLocationResponse getLiveLocation(@PathVariable String id);
}