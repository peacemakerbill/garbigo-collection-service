package com.garbigo.collection.dto;

import com.garbigo.collection.model.ScheduleFrequency;
import com.garbigo.collection.model.WasteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {

    private String id;
    private String clientId;
    private ScheduleFrequency frequency;
    private DayOfWeek dayOfWeek;
    private List<WasteType> wasteTypes;
    private LocationResponse location;
    private boolean active;
}
