package com.garbigo.collection.client;

import com.garbigo.collection.model.UserSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Falls back to auth-service for anything the local UserSummary cache doesn't cover. */
@FeignClient(name = "auth-service", url = "${services.auth.base-url}")
public interface AuthServiceClient {

    @GetMapping("/users/{id}")
    UserSummary getUserById(@PathVariable String id);
}