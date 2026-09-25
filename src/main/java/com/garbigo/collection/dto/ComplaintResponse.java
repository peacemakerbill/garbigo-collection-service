package com.garbigo.collection.dto;

import com.garbigo.collection.model.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {

    private String id;
    private String collectionRequestId;
    private String reporterId;
    private String description;
    private ComplaintStatus status;
    private Instant createdAt;
}
