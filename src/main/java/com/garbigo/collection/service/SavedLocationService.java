package com.garbigo.collection.service;

import com.garbigo.collection.dto.SavedLocationCreateRequest;
import com.garbigo.collection.dto.SavedLocationResponse;
import com.garbigo.collection.exception.CustomException;
import com.garbigo.collection.model.SavedLocation;
import com.garbigo.collection.repository.SavedLocationRepository;
import com.garbigo.collection.util.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedLocationService {

    private final SavedLocationRepository savedLocationRepository;

    public SavedLocationResponse create(String clientId, SavedLocationCreateRequest request) {
        SavedLocation saved = savedLocationRepository.save(
                SavedLocation.builder()
                        .clientId(clientId)
                        .label(request.getLabel())
                        .location(LocationMapper.toLocation(request.getLocation()))
                        .build()
        );
        return toResponse(saved);
    }

    public List<SavedLocationResponse> getMine(String clientId) {
        return savedLocationRepository.findByClientId(clientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void delete(String id, String clientId) {
        SavedLocation existing = findOwnedByOrThrow(id, clientId);
        savedLocationRepository.delete(existing);
    }

    /** Used by CollectionRequestService/SewageRequestService when a request references a saved location by id. */
    public SavedLocation findOwnedByOrThrow(String id, String clientId) {
        SavedLocation existing = savedLocationRepository.findById(id)
                .orElseThrow(() -> new CustomException("Saved location not found: " + id));
        if (!clientId.equals(existing.getClientId())) {
            throw new CustomException("Only the owning client can use this saved location");
        }
        return existing;
    }

    private SavedLocationResponse toResponse(SavedLocation entity) {
        return SavedLocationResponse.builder()
                .id(entity.getId())
                .label(entity.getLabel())
                .location(LocationMapper.toResponse(entity.getLocation()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}