package com.garbigo.collection.dto;

import com.garbigo.collection.model.SewageUrgency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SewageQuoteRequest {

    @NotNull
    @Positive
    private Integer tankVolumeLiters;

    @NotNull
    private SewageUrgency urgency;
}