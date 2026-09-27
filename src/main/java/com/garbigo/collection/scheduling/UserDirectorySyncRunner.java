package com.garbigo.collection.scheduling;

import com.garbigo.collection.service.UserSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Closes the staleness gap UserSummary's class docs flag: without this, a
 * role/active/archived change on an already-cached user is only ever seen
 * again on a cache miss. Runs hourly; a failure here (auth-service down)
 * just means this hour's refresh is skipped, not a startup/runtime error.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserDirectorySyncRunner {

    private final UserSummaryService userSummaryService;

    @Scheduled(cron = "0 0 * * * *")
    public void run() {
        try {
            userSummaryService.refreshFromAuthService();
        } catch (Exception e) {
            log.warn("Hourly user directory sync failed, will retry next hour: {}", e.getMessage());
        }
    }
}