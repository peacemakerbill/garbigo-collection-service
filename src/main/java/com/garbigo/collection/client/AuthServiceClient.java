package com.garbigo.collection.client;

import com.garbigo.collection.model.UserSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * getAllUsers(): confirmed - GET /internal/users, gated by hasRole("INTERNAL")
 * via the X-Internal-Api-Key header (attached globally by FeignConfig). A
 * user's own JWT does not work here. Unpaginated by design, so
 * UserSummaryService treats it as a full directory refresh.
 *
 * getLiveLocation(): a different access model, confirmed directly against
 * auth-service. GET /users/live-location/{userId} requires a real signed-in
 * user's own JWT (Authorization: Bearer ...) - the internal API key does
 * NOT work here, and there is no internal-service path to this data at
 * all. Callers must forward the requesting client's own bearer token (see
 * CollectorService) - legitimate, since it's the same JWT this service's
 * own JwtFilter already validated moments earlier.
 *
 * Response shape is genuinely heterogeneous (confirmed, not a guess this
 * time), so this returns a raw JsonNode rather than a fixed DTO: a
 * location-found response carries latitude/longitude/active plus profile
 * fields already covered by UserSummary; a "not found" response is STILL
 * 200 OK, not 404, and only has {message, userId, active: false}. Callers
 * must branch on "active", not assume one schema.
 *
 * Two caveats confirmed from auth-service's own team: locations expire
 * silently after a 2-hour Redis TTL (no way to distinguish "never shared"
 * from "went stale" from this response alone), and the "timestamp" field
 * is unreliable on one of auth-service's internal deserialization paths -
 * it can get overwritten with the read time rather than the actual last
 * push, so don't use it for staleness math.
 */
@FeignClient(name = "auth-service", url = "${services.auth.base-url}")
public interface AuthServiceClient {

    @GetMapping("/internal/users")
    List<UserSummary> getAllUsers();

    @GetMapping("/users/live-location/{userId}")
    JsonNode getLiveLocation(@PathVariable String userId, @RequestHeader("Authorization") String authorization);
}