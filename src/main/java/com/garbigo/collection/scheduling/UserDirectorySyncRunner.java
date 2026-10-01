package com.garbigo.collection.scheduling;

import com.garbigo.collection.service.UserSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Closes the staleness gap UserSummary's class docs flag: without this, a
 * role/active/archived change on an already-cached user is only ever seen
 * again on a cache miss. Runs on scheduling.user-directory-sync.cron
 * (default: hourly), AND once immediately on startup
 * (scheduling.user-directory-sync.run-on-startup, default true) - on a
 * fresh database, waiting for the first scheduled run means every login
 * gets zero role authorities (JwtFilter can't tell CLIENT from COLLECTOR
 * from an empty cache) until the clock catches up.
 *
 * The startup attempt specifically retries - up to
 * scheduling.user-directory-sync.startup-retry-attempts times (default 3),
 * waiting scheduling.user-directory-sync.startup-retry-delay-seconds
 * between tries (default 5) - since that's the one moment a failure is
 * actually likely (auth-service or RabbitMQ not quite up yet when this
 * service boots) and cheap to just try again for. The scheduled run
 * doesn't retry the same way; a failure there just waits for its next
 * regular tick, which is effectively its own retry.
 *
 * This blocks application startup for up to (attempts - 1) * delay
 * seconds in the worst case if every attempt fails - a bounded, deliberate
 * tradeoff (CommandLineRunner beans run synchronously before the app is
 * considered started), not an oversight.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserDirectorySyncRunner implements CommandLineRunner {

    private final UserSummaryService userSummaryService;

    @Value("${scheduling.user-directory-sync.run-on-startup}")
    private boolean runOnStartup;

    @Value("${scheduling.user-directory-sync.startup-retry-attempts}")
    private int startupRetryAttempts;

    @Value("${scheduling.user-directory-sync.startup-retry-delay-seconds}")
    private long startupRetryDelaySeconds;

    @Override
    public void run(String... args) {
        if (runOnStartup) {
            syncWithRetries();
        }
    }

    @Scheduled(cron = "${scheduling.user-directory-sync.cron}", zone = "UTC")
    public void scheduledRun() {
        sync("scheduled");
    }

    private void syncWithRetries() {
        for (int attempt = 1; attempt <= startupRetryAttempts; attempt++) {
            try {
                userSummaryService.refreshFromAuthService();
                if (attempt > 1) {
                    log.info("User directory sync (startup) succeeded on attempt {}/{}", attempt, startupRetryAttempts);
                }
                return;
            } catch (Exception e) {
                boolean lastAttempt = attempt == startupRetryAttempts;
                if (lastAttempt) {
                    log.warn("User directory sync (startup) failed after {} attempt(s), giving up until the next scheduled run: {}",
                            attempt, e.getMessage());
                } else {
                    log.warn("User directory sync (startup) failed on attempt {}/{}, retrying in {}s: {}",
                            attempt, startupRetryAttempts, startupRetryDelaySeconds, e.getMessage());
                    sleep(startupRetryDelaySeconds);
                }
            }
        }
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void sync(String trigger) {
        try {
            userSummaryService.refreshFromAuthService();
        } catch (Exception e) {
            log.warn("User directory sync ({}) failed, will retry on the next run: {}", trigger, e.getMessage());
        }
    }
}