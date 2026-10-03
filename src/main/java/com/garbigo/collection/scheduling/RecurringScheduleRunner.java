package com.garbigo.collection.scheduling;

import com.garbigo.collection.model.RecurringSchedule;
import com.garbigo.collection.model.ScheduleFrequency;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.model.WasteType;
import com.garbigo.collection.notification.MailService;
import com.garbigo.collection.repository.RecurringScheduleRepository;
import com.garbigo.collection.service.CollectionRequestService;
import com.garbigo.collection.service.UserSummaryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Runs once a day at scheduling.recurring-requests.run-at-hour-utc : run-at-minute-utc
 * (default 06:00 UTC). Deliberately a time of day, not an "every N minutes"
 * interval like the user sync: this is a once-a-day job, and run more often
 * it would re-send the same "tomorrow" reminder on every run (sendReminder has
 * no already-sent guard), generate a request for a schedule created after
 * its preferred time (already in the past), and fire right after midnight UTC
 * instead of mid-morning.
 *
 * For each active RecurringSchedule: generates a
 * CollectionRequest per waste type if today is due, or sends a reminder
 * email if tomorrow is due. "Due" uses a day-count threshold per frequency
 * rather than real calendar semantics for BIWEEKLY/MONTHLY - an
 * approximation, not calendar-exact (e.g. MONTHLY is "roughly every 28
 * days", not "same date each month").
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringScheduleRunner {

    private static final LocalTime DEFAULT_PREFERRED_TIME = LocalTime.of(9, 0);
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    private final RecurringScheduleRepository recurringScheduleRepository;
    private final CollectionRequestService collectionRequestService;
    private final UserSummaryService userSummaryService;
    private final MailService mailService;

    @Value("${scheduling.recurring-requests.run-at-hour-utc}")
    private int runAtHourUtc;

    @Value("${scheduling.recurring-requests.run-at-minute-utc}")
    private int runAtMinuteUtc;

    /** Fails startup with a clear message rather than a cryptic cron-parse error from Spring. */
    @PostConstruct
    void validateRunTime() {
        if (runAtHourUtc < 0 || runAtHourUtc > 23) {
            throw new IllegalStateException(
                    "scheduling.recurring-requests.run-at-hour-utc must be 0-23 (was " + runAtHourUtc + ")");
        }
        if (runAtMinuteUtc < 0 || runAtMinuteUtc > 59) {
            throw new IllegalStateException(
                    "scheduling.recurring-requests.run-at-minute-utc must be 0-59 (was " + runAtMinuteUtc + ")");
        }
    }

    // One plain placeholder; the cron itself is composed from the hour/minute
    // numbers in application.yml (scheduling.recurring-requests.schedule).
    @Scheduled(cron = "${scheduling.recurring-requests.schedule}", zone = "UTC")
    public void run() {
        long startedAt = System.nanoTime();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate tomorrow = today.plusDays(1);

        List<RecurringSchedule> schedules = recurringScheduleRepository.findByActiveTrue();
        log.info("Recurring schedules run started for {} ({} active schedules)", today, schedules.size());

        int schedulesGenerated = 0;
        int requestsCreated = 0;
        int remindersQueued = 0;
        int failed = 0;

        for (RecurringSchedule schedule : schedules) {
            try {
                if (isDueOn(schedule, today)) {
                    int created = generateRequests(schedule, today);
                    schedulesGenerated++;
                    requestsCreated += created;
                    log.info("Generated {} request(s) from recurring schedule {} (client {}) for {}",
                            created, schedule.getId(), schedule.getClientId(), today);
                } else if (isDueOn(schedule, tomorrow)) {
                    if (sendReminder(schedule, tomorrow)) {
                        remindersQueued++;
                        log.info("Queued pickup reminder for recurring schedule {} (client {}) due {}",
                                schedule.getId(), schedule.getClientId(), tomorrow);
                    } else {
                        log.warn("No reminder sent for recurring schedule {}: client {} not found in the user cache",
                                schedule.getId(), schedule.getClientId());
                    }
                }
            } catch (Exception e) {
                // One bad schedule shouldn't stop the rest of the run.
                failed++;
                log.warn("Failed to process recurring schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }

        log.info("Recurring schedules run finished in {} ms: {} generated ({} requests), {} reminders queued, {} failed",
                (System.nanoTime() - startedAt) / 1_000_000, schedulesGenerated, requestsCreated, remindersQueued, failed);
    }

    private boolean isDueOn(RecurringSchedule schedule, LocalDate date) {
        boolean dayMatches = schedule.getFrequency() == ScheduleFrequency.DAILY
                || schedule.getDayOfWeek() == date.getDayOfWeek();
        if (!dayMatches) {
            return false;
        }
        if (schedule.getLastGeneratedAt() == null) {
            return true;
        }
        long daysSinceLast = ChronoUnit.DAYS.between(
                schedule.getLastGeneratedAt().atZone(ZoneOffset.UTC).toLocalDate(), date);
        long minGapDays = switch (schedule.getFrequency()) {
            case DAILY -> 1;
            case WEEKLY -> 6;
            case BIWEEKLY -> 13;
            case MONTHLY -> 27;
        };
        return daysSinceLast >= minGapDays;
    }

    /** Returns how many requests were created (one per waste type on the schedule). */
    private int generateRequests(RecurringSchedule schedule, LocalDate date) {
        Instant scheduledAt = date.atTime(preferredTime(schedule)).toInstant(ZoneOffset.UTC);
        String notes = "Auto-generated from recurring schedule " + schedule.getId();

        for (WasteType wasteType : schedule.getWasteTypes()) {
            collectionRequestService.createFromSchedule(
                    schedule.getClientId(), wasteType, scheduledAt, schedule.getLocation(), notes);
        }

        schedule.setLastGeneratedAt(Instant.now());
        recurringScheduleRepository.save(schedule);
        return schedule.getWasteTypes().size();
    }

    /**
     * Returns false if the client isn't in the user cache, so the caller can
     * log it - that used to silently send nothing. True means the email was
     * handed to MailService, which sends asynchronously: a later SMTP failure
     * shows up as its own 'Failed to send' warning, not here.
     */
    private boolean sendReminder(RecurringSchedule schedule, LocalDate date) {
        Optional<UserSummary> found = userSummaryService.findById(schedule.getClientId());
        if (found.isEmpty()) {
            return false;
        }
        UserSummary client = found.get();

        String scheduledAtDisplay = DISPLAY_FORMAT.format(date.atTime(preferredTime(schedule)));
        String locationSummary = schedule.getLocation() == null
                ? "TBD"
                : String.join(", ", schedule.getLocation().getLocationName(), schedule.getLocation().getAddress());
        String wasteTypesDisplay = schedule.getWasteTypes().stream().map(Enum::name).collect(Collectors.joining(", "));

        mailService.sendUpcomingPickupReminder(
                client.getEmail(),
                client.preferredName(),
                "schedule-" + schedule.getId(),
                wasteTypesDisplay,
                scheduledAtDisplay,
                locationSummary
        );
        return true;
    }

    private LocalTime preferredTime(RecurringSchedule schedule) {
        return schedule.getPreferredTime() == null ? DEFAULT_PREFERRED_TIME : schedule.getPreferredTime();
    }
}