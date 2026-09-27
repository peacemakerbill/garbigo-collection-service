package com.garbigo.collection.repository;

import com.garbigo.collection.model.SewageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SewageRequestRepository extends MongoRepository<SewageRequest, String> {

    Page<SewageRequest> findByClientId(String clientId, Pageable pageable);

    Page<SewageRequest> findByCollectorId(String collectorId, Pageable pageable);
}