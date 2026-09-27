package com.garbigo.collection.repository;

import com.garbigo.collection.model.Complaint;
import com.garbigo.collection.model.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ComplaintRepository extends MongoRepository<Complaint, String> {

    Page<Complaint> findByReporterId(String reporterId, Pageable pageable);

    List<Complaint> findByCollectionRequestId(String collectionRequestId);

    long countByStatus(ComplaintStatus status);
}