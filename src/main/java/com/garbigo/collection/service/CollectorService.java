package com.garbigo.collection.service;

import com.garbigo.collection.client.AuthServiceClient;
import com.garbigo.collection.dto.CollectorResponse;
import com.garbigo.collection.model.Rating;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Backs GET /collectors - lets a client pick who to assign their own request to. */
@Service
@RequiredArgsConstructor
public class CollectorService {

    private static final String COLLECTOR_ROLE = "COLLECTOR";

    private final AuthServiceClient authServiceClient;
    private final UserSummaryService userSummaryService;
    private final RatingRepository ratingRepository;

    public List<CollectorResponse> listActiveCollectors() {
        List<UserSummary> users = authServiceClient.getAllUsers();
        users.forEach(userSummaryService::upsert);

        List<UserSummary> collectors = users.stream()
                .filter(user -> COLLECTOR_ROLE.equalsIgnoreCase(user.getRole())
                        && user.isActive()
                        && !user.isArchived())
                .toList();

        // One query for every collector in the list, grouped in Java - avoids N+1.
        List<String> collectorIds = collectors.stream().map(UserSummary::getId).toList();
        Map<String, List<Rating>> ratingsByCollector = ratingRepository.findByCollectorIdIn(collectorIds).stream()
                .collect(Collectors.groupingBy(Rating::getCollectorId));

        return collectors.stream()
                .map(user -> toResponse(user, ratingsByCollector.getOrDefault(user.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private CollectorResponse toResponse(UserSummary user, List<Rating> ratings) {
        Double averageRating = ratings.isEmpty()
                ? null
                : ratings.stream().mapToInt(Rating::getScore).average().orElse(0);

        return CollectorResponse.builder()
                .id(user.getId())
                .name(user.preferredName())
                .email(user.getEmail())
                .averageRating(averageRating)
                .ratingCount(ratings.size())
                .build();
    }
}