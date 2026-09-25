package com.garbigo.collection.service;

import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

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