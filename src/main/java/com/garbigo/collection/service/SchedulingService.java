package com.garbigo.collection.service;

import com.garbigo.collection.dto.LocationRequest;
import com.garbigo.collection.dto.LocationResponse;
import com.garbigo.collection.dto.ScheduleCreateRequest;
import com.garbigo.collection.dto.ScheduleResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.model.Location;
import com.garbigo.collection.model.RecurringSchedule;
import com.garbigo.collection.repository.RecurringScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for {@link RecurringSchedule}.
 *
 * <p>TODO: scaffolding-level implementation - no rule yet preventing a
 * client from creating overlapping schedules for the same day.
 */
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final RecurringScheduleRepository recurringScheduleRepository;

    public ScheduleResponse create(String clientId, ScheduleCreateRequest request) {
        RecurringSchedule saved = recurringScheduleRepository.save(
                RecurringSchedule.builder()
                        .clientId(clientId)
                        .frequency(request.getFrequency())
                        .dayOfWeek(request.getDayOfWeek())
                        .wasteTypes(request.getWasteTypes())
                        .location(toLocation(request.getLocation()))
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
        existing.setWasteTypes(request.getWasteTypes());
        existing.setLocation(toLocation(request.getLocation()));
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

    private Location toLocation(LocationRequest request) {
        return Location.builder()
                .locationName(request.getLocationName())
                .address(request.getAddress())
                .landmark(request.getLandmark())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
    }

    private LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }
        return LocationResponse.builder()
                .locationName(location.getLocationName())
                .address(location.getAddress())
                .landmark(location.getLandmark())
                .city(location.getCity())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }

    private ScheduleResponse toResponse(RecurringSchedule entity) {
        return ScheduleResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .frequency(entity.getFrequency())
                .dayOfWeek(entity.getDayOfWeek())
                .wasteTypes(entity.getWasteTypes())
                .location(toLocationResponse(entity.getLocation()))
                .active(entity.isActive())
                .build();
    }
}