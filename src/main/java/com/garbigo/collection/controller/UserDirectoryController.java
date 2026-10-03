package com.garbigo.collection.controller;

import com.garbigo.collection.dto.MessageResponse;
import com.garbigo.collection.service.UserSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deliberately requires only a valid JWT (SecurityConfig's default
 * .anyRequest().authenticated()) - no specific role, and no @PreAuthorize
 * here at all. That's not an oversight: this exists to fix the exact
 * situation where it would otherwise be needed. A user whose role was
 * never cached (fresh deployment, or an account older than
 * collection-service's own uptime) gets authenticated by JwtFilter but
 * with zero granted authorities, so every role-gated endpoint - including
 * an admin-only one - would 403 them. Authentication alone (no authority
 * check) is enough to call this, which is what breaks the circularity.
 *
 * Not destructive or sensitive - just re-runs the same full directory
 * pull UserDirectorySyncRunner already does hourly, on demand instead of
 * waiting for the clock.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserDirectoryController {

    private final UserSummaryService userSummaryService;

    @PostMapping("/resync")
    public ResponseEntity<MessageResponse> resync() {
        int synced = userSummaryService.refreshFromAuthService();
        return ResponseEntity.ok(new MessageResponse("Synced " + synced + " users from auth-service"));
    }
}