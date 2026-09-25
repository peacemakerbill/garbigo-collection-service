package com.garbigo.collection.service;

import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Reads and writes the local, read-only {@link UserSummary} cache.
 *
 * <p>Writes happen from
 * {@link com.garbigo.collection.messaging.UserCreatedEventListener} only.
 * This is also where {@link com.garbigo.collection.security.JwtFilter}
 * looks up a caller's role for authorization, since the JWT itself carries
 * no role claim.
 */
@Service
@RequiredArgsConstructor
public class UserSummaryService {

    private final UserSummaryRepository userSummaryRepository;

    public Optional<UserSummary> findById(String userId) {
        return userSummaryRepository.findById(userId);
    }

    public UserSummary upsert(UserSummary userSummary) {
        return userSummaryRepository.save(userSummary);
    }
}
