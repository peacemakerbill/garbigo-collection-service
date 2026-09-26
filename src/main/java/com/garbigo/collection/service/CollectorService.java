package com.garbigo.collection.service;

import com.garbigo.collection.client.AuthServiceClient;
import com.garbigo.collection.dto.CollectorResponse;
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

    public List<CollectorResponse> listActiveCollectors() {
        return authServiceClient.getAllUsers().stream()
                .filter(user -> COLLECTOR_ROLE.equalsIgnoreCase(user.getRole()) && user.isActive())
                .map(user -> CollectorResponse.builder()
                        .id(user.getId())
                        .displayUsername(user.getDisplayUsername())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toList());
    }
}