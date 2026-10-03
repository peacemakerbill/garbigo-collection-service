package com.garbigo.collection.controller;

import com.garbigo.collection.dto.MessageResponse;
import com.garbigo.collection.service.UserSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADMIN only. This used to require just a valid JWT, on purpose: a user
 * whose role was never cached had zero authorities, so a role-gated resync
 * would have locked out exactly the person who needed it to fix that. That
 * reason is gone - JwtFilter now refreshes the cache on a miss
 * (UserSummaryService.resolve) before the controller runs, so by the time
 * this check happens an admin's role has already been resolved.
 *
 * Worth gating regardless: it triggers an unpaginated, full-directory
 * pull from auth-service, which any signed-in account could otherwise
 * hammer. Still not destructive - it's the same upsert-only sync the
 * hourly UserDirectorySyncRunner does - so ADMIN is about who gets to
 * trigger it, not about what it exposes.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserDirectoryController {

    private final UserSummaryService userSummaryService;

    @PostMapping("/resync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> resync() {
        int synced = userSummaryService.refreshFromAuthService();
        return ResponseEntity.ok(new MessageResponse("Synced " + synced + " users from auth-service"));
    }
}