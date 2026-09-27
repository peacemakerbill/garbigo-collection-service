package com.garbigo.collection.repository;

import com.garbigo.collection.model.SavedLocation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SavedLocationRepository extends MongoRepository<SavedLocation, String> {

    List<SavedLocation> findByClientId(String clientId);
}