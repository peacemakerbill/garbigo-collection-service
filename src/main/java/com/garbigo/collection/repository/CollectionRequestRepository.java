package com.garbigo.collection.repository;

import com.garbigo.collection.model.CollectionRequest;
import com.garbigo.collection.model.CollectionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CollectionRequestRepository extends MongoRepository<CollectionRequest, String> {

    List<CollectionRequest> findByClientId(String clientId);

    List<CollectionRequest> findByCollectorId(String collectorId);

    List<CollectionRequest> findByCollectorIdAndStatus(String collectorId, CollectionStatus status);
}