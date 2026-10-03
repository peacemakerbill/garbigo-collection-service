package com.garbigo.collection.service;

import com.garbigo.collection.dto.ScheduleCreateRequest;
import com.garbigo.collection.dto.ScheduleResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.exception.NotFoundException;
import com.garbigo.collection.model.RecurringSchedule;
import com.garbigo.collection.repository.RecurringScheduleRepository;
import com.garbigo.collection.util.LocationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulingService {

    private static final LocalTime DEFAULT_PREFERRED_TIME = LocalTime.of(9, 0);

    private final RecurringScheduleRepository recurringScheduleRepository;
    private final MongoTemplate mongoTemplate;

    public ScheduleResponse create(String clientId, ScheduleCreateRequest request) {
        RecurringSchedule saved = recurringScheduleRepository.save(
                RecurringSchedule.builder()
                        .clientId(clientId)
                        .frequency(request.getFrequency())
                        .dayOfWeek(request.getDayOfWeek())
                        .preferredTime(request.getPreferredTime() == null ? DEFAULT_PREFERRED_TIME : request.getPreferredTime())
                        .wasteTypes(request.getWasteTypes())
                        .location(LocationMapper.toLocation(request.getLocation()))
                        .active(true)
                        .build()
        );
        return toResponse(saved);
    }

    public List<ScheduleResponse> getMine(String clientId) {
        return recurringScheduleRepository.findByClientId(clientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ScheduleResponse update(String id, String clientId, ScheduleCreateRequest request) {
        RecurringSchedule existing = findOwnedByOrThrow(id, clientId);
        existing.setFrequency(request.getFrequency());
        existing.setDayOfWeek(request.getDayOfWeek());
        existing.setPreferredTime(request.getPreferredTime() == null ? DEFAULT_PREFERRED_TIME : request.getPreferredTime());
        existing.setWasteTypes(request.getWasteTypes());
        existing.setLocation(LocationMapper.toLocation(request.getLocation()));
        return toResponse(recurringScheduleRepository.save(existing));
    }

    public void delete(String id, String clientId) {
        RecurringSchedule existing = findOwnedByOrThrow(id, clientId);
        recurringScheduleRepository.delete(existing);
    }

    // ------------------------------------------------------------------
    // Admin operations (see AdminScheduleController).
    // ------------------------------------------------------------------

    public Page<ScheduleResponse> adminSearch(String clientId, Boolean active, Pageable pageable) {
        Query query = new Query();
        if (StringUtils.hasText(clientId)) {
            query.addCriteria(Criteria.where("clientId").is(clientId.trim()));
        }
        if (active != null) {
            query.addCriteria(Criteria.where("active").is(active));
        }
        long total = mongoTemplate.count(query, RecurringSchedule.class);
        List<ScheduleResponse> content = mongoTemplate.find(query.with(pageable), RecurringSchedule.class).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, total);
    }

    /** Pauses or resumes a schedule without deleting it - RecurringScheduleRunner only reads active ones. */
    public ScheduleResponse adminSetActive(String adminId, String id, boolean active) {
        RecurringSchedule existing = findOrThrow(id);
        if (existing.isActive() == active) {
            throw new CustomException("This schedule is already " + (active ? "active" : "inactive"));
        }
        existing.setActive(active);
        RecurringSchedule saved = recurringScheduleRepository.save(existing);
        log.info("Admin {} set recurring schedule {} active={}", adminId, id, active);
        return toResponse(saved);
    }

    /**
     * Safe to delete outright: requests already generated from a schedule are
     * independent records and stay. They only carry the schedule's id in their
     * notes text.
     */
    public void adminDelete(String adminId, String id) {
        RecurringSchedule existing = findOrThrow(id);
        recurringScheduleRepository.delete(existing);
        log.info("Admin {} deleted recurring schedule {} (client {})", adminId, id, existing.getClientId());
    }

    private RecurringSchedule findOrThrow(String id) {
        return recurringScheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Schedule not found: " + id));
    }

    private RecurringSchedule findOwnedByOrThrow(String id, String clientId) {
        RecurringSchedule existing = recurringScheduleRepository.findById(id)
                .orElseThrow(() -> new CustomException("Schedule not found: " + id));
        if (!clientId.equals(existing.getClientId())) {
            throw new CustomException("Only the owning client can modify this schedule");
        }
        return existing;
    }

    private ScheduleResponse toResponse(RecurringSchedule entity) {
        return ScheduleResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .frequency(entity.getFrequency())
                .dayOfWeek(entity.getDayOfWeek())
                .preferredTime(entity.getPreferredTime())
                .wasteTypes(entity.getWasteTypes())
                .location(LocationMapper.toResponse(entity.getLocation()))
                .active(entity.isActive())
                .build();
    }
}