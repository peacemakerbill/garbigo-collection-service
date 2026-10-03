package com.garbigo.collection.scheduling;

import com.garbigo.collection.service.UserSummaryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Closes the staleness gap UserSummary's class docs flag: without this, a
 * role/active/archived change on an already-cached user is only ever seen
 * again on a cache miss. Runs every
 * scheduling.user-directory-sync.interval-minutes (default: 10), AND once immediately on startup
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

    @Value("${scheduling.user-directory-sync.interval-minutes}")
    private long intervalMinutes;

    @Value("${scheduling.user-directory-sync.run-on-startup}")
    private boolean runOnStartup;

    @Value("${scheduling.user-directory-sync.startup-retry-attempts}")
    private int startupRetryAttempts;

    @Value("${scheduling.user-directory-sync.startup-retry-delay-seconds}")
    private long startupRetryDelaySeconds;

    /**
     * Fails startup on a nonsensical interval rather than letting it through:
     * 0 would make fixedDelay re-run the sync back-to-back with no pause at
     * all, hammering auth-service in a tight loop.
     */
    @PostConstruct
    void validateInterval() {
        if (intervalMinutes < 1) {
            throw new IllegalStateException(
                    "scheduling.user-directory-sync.interval-minutes must be at least 1 (was " + intervalMinutes + ")");
        }
    }

    @Override
    public void run(String... args) {
        if (runOnStartup) {
            syncWithRetries();
        }
    }

    /**
     * fixedDelay (not fixedRate): the interval counts from when the previous
     * sync finished, so a slow one never overlaps the next. The first tick is
     * one full interval after startup (initialDelay) because the startup run
     * above already covers "now".
     */
    @Scheduled(fixedDelayString = "${scheduling.user-directory-sync.interval-minutes}",
            initialDelayString = "${scheduling.user-directory-sync.interval-minutes}",
            timeUnit = TimeUnit.MINUTES)
    public void scheduledRun() {
        sync("scheduled");
    }

    private void syncWithRetries() {
        long startedAt = System.nanoTime();
        for (int attempt = 1; attempt <= startupRetryAttempts; attempt++) {
            try {
                userSummaryService.refreshFromAuthService();
                log.info("User directory sync (startup) completed in {} ms (attempt {}/{})",
                        elapsedMillis(startedAt), attempt, startupRetryAttempts);
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

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void sync(String trigger) {
        long startedAt = System.nanoTime();
        try {
            userSummaryService.refreshFromAuthService();
            log.info("User directory sync ({}) completed in {} ms", trigger, elapsedMillis(startedAt));
        } catch (Exception e) {
            log.warn("User directory sync ({}) failed, will retry on the next run: {}", trigger, e.getMessage());
        }
    }
}