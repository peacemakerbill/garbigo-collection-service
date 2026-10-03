package com.garbigo.collection.service;

import com.garbigo.collection.client.AuthServiceClient;
import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSummaryService {

    private final UserSummaryRepository userSummaryRepository;
    private final AuthServiceClient authServiceClient;

    public Optional<UserSummary> findById(String userId) {
        return userSummaryRepository.findById(userId);
    }

    public UserSummary upsert(UserSummary userSummary) {
        userSummaryRepository.findById(userSummary.getId())
                .ifPresent(existing -> keepOptionalFieldsIfMissing(userSummary, existing));
        return userSummaryRepository.save(userSummary);
    }

    /** Bulk version of upsert: one read and one write for the whole batch instead of a read and a write per user. */
    public void upsertAll(Collection<UserSummary> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Map<String, UserSummary> existing = findAllByIds(
                users.stream().map(UserSummary::getId).filter(Objects::nonNull).toList());
        for (UserSummary user : users) {
            UserSummary previous = existing.get(user.getId());
            if (previous != null) {
                keepOptionalFieldsIfMissing(user, previous);
            }
        }
        userSummaryRepository.saveAll(users);
    }

    /**
     * Saving replaces the whole document, so without this a sync whose payload
     * doesn't carry phoneNumber or profilePictureUrl (an event, or a directory
     * response that lacks them) would blank values an earlier path had stored.
     * The cost: if auth-service ever sends these as explicitly cleared, the old
     * value is kept - once it's confirmed GET /internal/users always includes
     * them, drop this and let the incoming value win.
     */
    private void keepOptionalFieldsIfMissing(UserSummary incoming, UserSummary existing) {
        if (!StringUtils.hasText(incoming.getPhoneNumber())) {
            incoming.setPhoneNumber(existing.getPhoneNumber());
        }
        if (!StringUtils.hasText(incoming.getProfilePictureUrl())) {
            incoming.setProfilePictureUrl(existing.getProfilePictureUrl());
        }
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

    /**
     * Returns how many users were synced and logs it - a successful
     * refresh used to be completely silent, so a working sync (startup,
     * periodic, cache-miss, or POST /users/resync) looked identical to one
     * that did nothing.
     */
    public int refreshFromAuthService() {
        List<UserSummary> users = authServiceClient.getAllUsers();
        if (users == null) {
            users = List.of();
        }
        upsertAll(users);
        log.info("User directory refreshed from auth-service: {} users", users.size());
        return users.size();
    }

    /** Count from the local cache, not a live auth-service call - used by AdminStatsService. */
    public long countCachedCollectors() {
        return userSummaryRepository.countByRoleIgnoreCase("COLLECTOR");
    }

    public long countCachedClients() {
        return userSummaryRepository.countByRoleIgnoreCase("CLIENT");
    }

    /**
     * One query for many users, keyed by id - for listings that need names
     * without a lookup per row. Cache-only on purpose: unlike resolve(), a
     * miss doesn't trigger a full directory refresh from auth-service, which
     * a page of rows with several unknown users would otherwise repeat.
     */
    public Map<String, UserSummary> findAllByIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<String, UserSummary> byId = new HashMap<>();
        userSummaryRepository.findAllById(userIds).forEach(user -> byId.put(user.getId(), user));
        return byId;
    }
}