package com.garbigo.collection.scheduling;

import com.garbigo.collection.service.UserSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Closes the staleness gap UserSummary's class docs flag: without this, a
 * role/active/archived change on an already-cached user is only ever seen
 * again on a cache miss. Runs on scheduling.user-directory-sync.cron (default:
 * hourly); a failure here (auth-service down) just means that run is skipped,
 * not a startup/runtime error.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserDirectorySyncRunner {

    private final UserSummaryService userSummaryService;

    @Scheduled(cron = "${scheduling.user-directory-sync.cron}", zone = "UTC")
    public void run() {
        try {
            userSummaryService.refreshFromAuthService();
        } catch (Exception e) {
            log.warn("User directory sync failed, will retry on the next run: {}", e.getMessage());
        }
    }
}