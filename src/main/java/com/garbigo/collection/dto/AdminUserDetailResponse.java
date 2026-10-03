package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One user plus their activity in this service. Counts are zero for roles that don't apply (e.g. jobs for a client). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {

    private AdminUserResponse user;

    private long collectionRequestsAsClient;
    private long sewageRequestsAsClient;

    private long collectionJobsAsCollector;
    private long sewageJobsAsCollector;
    private long completedJobsAsCollector;

    private long complaintsFiled;

    /** Null when the user has no ratings. */
    private Double averageRating;
    private long ratingCount;
}