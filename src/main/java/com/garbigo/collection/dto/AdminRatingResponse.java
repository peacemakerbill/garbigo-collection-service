package com.garbigo.collection.dto;

import com.garbigo.collection.model.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Unlike RatingResponse, includes who left the rating - for moderation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRatingResponse {

    private String id;
    private String collectionRequestId;
    private String reviewerId;
    private String collectorId;
    private RequestType requestType;
    private int score;
    private String comment;
    private Instant createdAt;
}