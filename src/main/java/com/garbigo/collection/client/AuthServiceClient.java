package com.garbigo.collection.client;

import com.garbigo.collection.model.UserSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Calls back into garbigo-auth-service for anything the local
 * {@link UserSummary} cache doesn't cover - e.g. a full profile to display
 * alongside a collection request. Every call carries the shared
 * INTERNAL_API_KEY via {@link com.garbigo.collection.config.FeignConfig},
 * which auth-service's InternalApiKeyFilter already accepts.
 *
 * <p>{@code @PathVariable} below has no explicit name - Spring Boot's
 * parent POM compiles with {@code -parameters}, so the parameter's own
 * name ({@code id}) is enough to match {@code {id}} in the path.
 *
 * <p>TODO: the endpoint below ({@code GET /users/{id}}) is a guess at
 * auth-service's shape, matching the user-management endpoints listed in
 * its Postman collection - confirm the real path once auth-service's
 * actual controller is available. The return type will likely need to
 * become a dedicated response DTO rather than reusing {@link UserSummary}
 * directly, since auth-service's full profile almost certainly returns more
 * fields than this service's local cache does.
 */
@FeignClient(name = "auth-service", url = "${services.auth.base-url}")
public interface AuthServiceClient {

    @GetMapping("/users/{id}")
    UserSummary getUserById(@PathVariable String id);
}