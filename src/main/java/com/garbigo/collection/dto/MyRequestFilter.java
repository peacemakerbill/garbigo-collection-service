package com.garbigo.collection.dto;

import com.garbigo.collection.model.CollectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Optional filters on a client's or collector's own request lists. status wins
 * over active if both are given; active=true means still in play (PENDING,
 * ASSIGNED, ACCEPTED, IN_PROGRESS), active=false means finished (COMPLETED,
 * CANCELLED, DISPUTED). The date range is on when the pickup is scheduled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyRequestFilter {

    private CollectionStatus status;
    private Boolean active;
    private Instant scheduledFrom;
    private Instant scheduledTo;
}