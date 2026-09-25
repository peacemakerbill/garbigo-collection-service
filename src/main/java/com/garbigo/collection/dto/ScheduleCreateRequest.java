package com.garbigo.collection.dto;

import com.garbigo.collection.model.ScheduleFrequency;
import com.garbigo.collection.model.WasteType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ScheduleCreateRequest {

    @NotNull
    private ScheduleFrequency frequency;

    @NotNull
    private DayOfWeek dayOfWeek;

    @NotEmpty
    private List<WasteType> wasteTypes;

    @Valid
    @NotNull
    private LocationRequest location;
}
