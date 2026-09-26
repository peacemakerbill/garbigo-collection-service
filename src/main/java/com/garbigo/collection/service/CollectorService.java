package com.garbigo.collection.service;

import com.garbigo.collection.client.AuthServiceClient;
import com.garbigo.collection.dto.CollectorResponse;
import com.garbigo.collection.model.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** Backs GET /collectors - lets a client pick who to assign their own request to. */
@Service
@RequiredArgsConstructor
public class CollectorService {

    private static final String COLLECTOR_ROLE = "COLLECTOR";

    private final AuthServiceClient authServiceClient;
    private final UserSummaryService userSummaryService;

    public List<CollectorResponse> listActiveCollectors() {
        List<UserSummary> users = authServiceClient.getAllUsers();
        users.forEach(userSummaryService::upsert);

        return users.stream()
                .filter(user -> COLLECTOR_ROLE.equalsIgnoreCase(user.getRole())
                        && user.isActive()
                        && !user.isArchived())
                .map(user -> CollectorResponse.builder()
                        .id(user.getId())
                        .name(user.preferredName())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toList());
    }
}