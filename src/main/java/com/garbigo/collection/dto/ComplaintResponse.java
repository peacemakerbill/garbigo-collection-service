package com.garbigo.collection.dto;

import com.garbigo.collection.model.RequestType;
import com.garbigo.collection.model.ComplaintCategory;
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
    private RequestType requestType;
    private String reporterId;
    private String reporterName;
    private String reporterEmail;
    private ComplaintCategory category;
    private String description;
    private ComplaintStatus status;
    private Instant createdAt;
}