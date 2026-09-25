package com.garbigo.collection.repository;

import com.garbigo.collection.model.Complaint;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ComplaintRepository extends MongoRepository<Complaint, String> {

    List<Complaint> findByReporterId(String reporterId);

    List<Complaint> findByCollectionRequestId(String collectionRequestId);
}