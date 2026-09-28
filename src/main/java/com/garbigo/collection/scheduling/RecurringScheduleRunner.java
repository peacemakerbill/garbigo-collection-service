package com.garbigo.collection.scheduling;

import com.garbigo.collection.model.RecurringSchedule;
import com.garbigo.collection.model.ScheduleFrequency;
import com.garbigo.collection.model.WasteType;
import com.garbigo.collection.notification.MailService;
import com.garbigo.collection.repository.RecurringScheduleRepository;
import com.garbigo.collection.service.CollectionRequestService;
import com.garbigo.collection.service.UserSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

/**
 * Runs on scheduling.recurring-requests.cron (default: daily at 06:00 UTC).
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

    @Scheduled(cron = "${scheduling.recurring-requests.cron}", zone = "UTC")
    public void run() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate tomorrow = today.plusDays(1);

        for (RecurringSchedule schedule : recurringScheduleRepository.findByActiveTrue()) {
            try {
                if (isDueOn(schedule, today)) {
                    generateRequests(schedule, today);
                } else if (isDueOn(schedule, tomorrow)) {
                    sendReminder(schedule, tomorrow);
                }
            } catch (Exception e) {
                // One bad schedule shouldn't stop the rest of the run.
                log.warn("Failed to process recurring schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }
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

    private void generateRequests(RecurringSchedule schedule, LocalDate date) {
        Instant scheduledAt = date.atTime(preferredTime(schedule)).toInstant(ZoneOffset.UTC);
        String notes = "Auto-generated from recurring schedule " + schedule.getId();

        for (WasteType wasteType : schedule.getWasteTypes()) {
            collectionRequestService.createFromSchedule(
                    schedule.getClientId(), wasteType, scheduledAt, schedule.getLocation(), notes);
        }

        schedule.setLastGeneratedAt(Instant.now());
        recurringScheduleRepository.save(schedule);
    }

    private void sendReminder(RecurringSchedule schedule, LocalDate date) {
        userSummaryService.findById(schedule.getClientId()).ifPresent(client -> {
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
        });
    }

    private LocalTime preferredTime(RecurringSchedule schedule) {
        return schedule.getPreferredTime() == null ? DEFAULT_PREFERRED_TIME : schedule.getPreferredTime();
    }
}