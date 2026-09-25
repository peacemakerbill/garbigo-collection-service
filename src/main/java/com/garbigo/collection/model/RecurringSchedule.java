package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.util.List;

@Document(collection = "recurring_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringSchedule {

    @Id
    private String id;

    private String clientId;
    private ScheduleFrequency frequency;
    private DayOfWeek dayOfWeek;
    private List<WasteType> wasteTypes;
    private Location location;
    private boolean active;
}