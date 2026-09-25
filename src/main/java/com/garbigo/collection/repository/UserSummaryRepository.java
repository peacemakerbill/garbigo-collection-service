package com.garbigo.collection.repository;

import com.garbigo.collection.model.UserSummary;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserSummaryRepository extends MongoRepository<UserSummary, String> {
}