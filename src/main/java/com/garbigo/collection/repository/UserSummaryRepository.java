package com.garbigo.collection.repository;

import com.garbigo.collection.model.UserSummary;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * id IS the auth-service user id (see {@link UserSummary}), so no extra
 * finder methods are needed beyond what MongoRepository already provides
 * (findById, existsById, ...).
 */
public interface UserSummaryRepository extends MongoRepository<UserSummary, String> {
}
