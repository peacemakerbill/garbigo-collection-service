package com.garbigo.collection.service;

import com.garbigo.collection.dto.ScheduleCreateRequest;
import com.garbigo.collection.dto.ScheduleResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.model.RecurringSchedule;
import com.garbigo.collection.repository.RecurringScheduleRepository;
import com.garbigo.collection.util.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private static final LocalTime DEFAULT_PREFERRED_TIME = LocalTime.of(9, 0);

    private final RecurringScheduleRepository recurringScheduleRepository;

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