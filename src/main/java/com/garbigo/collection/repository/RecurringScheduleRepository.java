package com.garbigo.collection.repository;

import com.garbigo.collection.model.RecurringSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RecurringScheduleRepository extends MongoRepository<RecurringSchedule, String> {

    List<RecurringSchedule> findByClientId(String clientId);

    List<RecurringSchedule> findByClientIdAndActiveTrue(String clientId);
}
