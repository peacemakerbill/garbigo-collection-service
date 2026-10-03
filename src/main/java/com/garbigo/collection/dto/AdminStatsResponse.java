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
public class AdminStatsResponse {

    // The original fields, unchanged, so anything already reading them keeps working.
    private long collectionsPending;
    private long collectionsAssigned;
    private long collectionsCompleted;
    private long collectionsCancelled;
    private long complaintsOpen;
    private long complaintsResolved;
    private long registeredCollectorsCached;

    // Added: the full breakdowns the flat fields above only partly cover.
    private long complaintsInReview;
    private long registeredClientsCached;
    private long activeSchedules;
    private long totalRatings;

    /** Across every rating; null when there are none. */
    private Double averageRating;

    /** Every status, including zeros, in lifecycle order. */
    private Map<String, Long> collectionsByStatus;
    private Map<String, Long> sewageByStatus;

    /** Collection and sewage requests combined. */
    private Map<String, Long> paymentStatusCounts;
}