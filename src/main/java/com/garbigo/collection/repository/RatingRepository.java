package com.garbigo.collection.repository;

import com.garbigo.collection.model.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RatingRepository extends MongoRepository<Rating, String> {

    boolean existsByCollectionRequestId(String collectionRequestId);

    /** One query for a whole page of collectors, grouped in Java - avoids N+1 when listing collectors. */
    List<Rating> findByCollectorIdIn(List<String> collectorIds);
}