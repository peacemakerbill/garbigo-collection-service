package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRequestCountResponse {

    /** UTC calendar day. */
    private LocalDate date;
    private long collections;
    private long sewage;
    private long total;
}