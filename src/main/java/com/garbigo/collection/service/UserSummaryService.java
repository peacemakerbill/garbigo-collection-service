package com.garbigo.collection.service;

import com.garbigo.collection.client.AuthServiceClient;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSummaryService {

    private final UserSummaryRepository userSummaryRepository;
    private final AuthServiceClient authServiceClient;

    public Optional<UserSummary> findById(String userId) {
        return userSummaryRepository.findById(userId);
    }

    public UserSummary upsert(UserSummary userSummary) {
        return userSummaryRepository.save(userSummary);
    }

    /**
     * Cache-first. On a miss, refreshes the whole local cache from
     * auth-service's GET /internal/users and retries once - that endpoint
     * is unpaginated by design, meant for exactly this kind of directory
     * sync rather than per-user lookups. A connectivity failure
     * (feign.RetryableException) propagates as-is; callers decide whether
     * that's essential (let it surface as the global 503) or supplementary
     * (catch it and degrade).
     */
    public Optional<UserSummary> resolve(String userId) {
        Optional<UserSummary> cached = findById(userId);
        if (cached.isPresent()) {
            return cached;
        }
        refreshFromAuthService();
        return findById(userId);
    }

    public void refreshFromAuthService() {
        userSummaryRepository.saveAll(authServiceClient.getAllUsers());
    }
}