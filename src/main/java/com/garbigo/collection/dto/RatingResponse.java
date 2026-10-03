package com.garbigo.collection.dto;

import com.garbigo.collection.model.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {

    private String id;
    private String collectionRequestId;
    private String collectorId;
    private RequestType requestType;
    private int score;
    private String comment;
    private Instant createdAt;
}