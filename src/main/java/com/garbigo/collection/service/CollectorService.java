package com.garbigo.collection.service;

import com.garbigo.collection.client.AuthServiceClient;
import com.garbigo.collection.dto.CollectorResponse;
import com.garbigo.collection.model.Rating;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Backs GET /collectors and /collectors/nearby - lets a client pick who to assign their own request to. */
@Service
@RequiredArgsConstructor
public class CollectorService {

    private static final String COLLECTOR_ROLE = "COLLECTOR";
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final AuthServiceClient authServiceClient;
    private final UserSummaryService userSummaryService;
    private final RatingRepository ratingRepository;

    public List<CollectorResponse> listActiveCollectors() {
        List<UserSummary> collectors = activeCollectors();
        Map<String, List<Rating>> ratingsByCollector = ratingsByCollector(collectors);

        return collectors.stream()
                .map(user -> toResponse(user, ratingsByCollector.getOrDefault(user.getId(), List.of()), null))
                .collect(Collectors.toList());
    }

    /**
     * Same active-collector set as listActiveCollectors(), sorted by
     * distance and limited to radiusKm. auth-service's live-location
     * endpoint only accepts a real user's JWT (no internal-key path
     * exists), so this forwards the calling client's own bearer token -
     * one Feign call per collector, since there's no bulk live-location
     * endpoint. A collector with no active location (never shared one, or
     * their 2-hour TTL silently expired - the two are indistinguishable
     * from this response) is just excluded, not treated as an error.
     */
    public List<CollectorResponse> listNearbyCollectors(String authorization, double latitude, double longitude, double radiusKm) {
        List<UserSummary> collectors = activeCollectors();
        Map<String, List<Rating>> ratingsByCollector = ratingsByCollector(collectors);

        return collectors.stream()
                .map(user -> {
                    JsonNode location = authServiceClient.getLiveLocation(user.getId(), authorization);
                    if (location == null || !location.path("active").asBoolean(false)) {
                        return null;
                    }
                    double collectorLat = location.get("latitude").asDouble();
                    double collectorLng = location.get("longitude").asDouble();
                    double distanceKm = haversineKm(latitude, longitude, collectorLat, collectorLng);
                    if (distanceKm > radiusKm) {
                        return null;
                    }
                    return toResponse(user, ratingsByCollector.getOrDefault(user.getId(), List.of()), distanceKm);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(CollectorResponse::getDistanceKm))
                .collect(Collectors.toList());
    }

    private List<UserSummary> activeCollectors() {
        List<UserSummary> users = authServiceClient.getAllUsers();
        userSummaryService.upsertAll(users);

        return users.stream()
                .filter(user -> COLLECTOR_ROLE.equalsIgnoreCase(user.getRole())
                        && user.isActive()
                        && !user.isArchived())
                .toList();
    }

    private Map<String, List<Rating>> ratingsByCollector(List<UserSummary> collectors) {
        List<String> collectorIds = collectors.stream().map(UserSummary::getId).toList();
        return ratingRepository.findByCollectorIdIn(collectorIds).stream()
                .collect(Collectors.groupingBy(Rating::getCollectorId));
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private CollectorResponse toResponse(UserSummary user, List<Rating> ratings, Double distanceKm) {
        Double averageRating = ratings.isEmpty()
                ? null
                : ratings.stream().mapToInt(Rating::getScore).average().orElse(0);

        return CollectorResponse.builder()
                .id(user.getId())
                .name(user.preferredName())
                .email(user.getEmail())
                .averageRating(averageRating)
                .ratingCount(ratings.size())
                .distanceKm(distanceKm)
                .build();
    }
}