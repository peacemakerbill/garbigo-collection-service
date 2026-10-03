package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectorPerformanceResponse {

    private String collectorId;

    /** From the local user cache; null if the collector isn't in it. */
    private String name;

    /** Across collection and sewage requests. */
    private long completedJobs;

    /** Currently ASSIGNED, ACCEPTED or IN_PROGRESS. */
    private long openJobs;
    private long disputedJobs;

    /** Across collection and sewage ratings. Null when there are none. */
    private Double averageRating;
    private long ratingCount;
}