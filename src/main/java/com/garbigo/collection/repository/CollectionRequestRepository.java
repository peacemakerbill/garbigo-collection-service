package com.garbigo.collection.repository;

import com.garbigo.collection.model.CollectionRequest;
import com.garbigo.collection.model.CollectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CollectionRequestRepository extends MongoRepository<CollectionRequest, String> {

    Page<CollectionRequest> findByClientId(String clientId, Pageable pageable);

    Page<CollectionRequest> findByCollectorId(String collectorId, Pageable pageable);

    List<CollectionRequest> findByCollectorIdAndStatus(String collectorId, CollectionStatus status);

    long countByStatus(CollectionStatus status);
}