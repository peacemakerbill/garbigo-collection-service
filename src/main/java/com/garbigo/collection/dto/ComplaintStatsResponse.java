package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatsResponse {

    private long total;
    private Map<String, Long> byStatus;
    private Map<String, Long> byCategory;

    /** Unresolved complaints older than this many days count as stale. */
    private int staleAfterDays;
    private long staleUnresolved;
}