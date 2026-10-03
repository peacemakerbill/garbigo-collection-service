package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** One rating per completed CollectionRequest - not attached to sewage requests yet. */
@Document(collection = "ratings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    @Id
    private String id;

    @Indexed(unique = true)
    private String collectionRequestId;

    private String reviewerId;
    private String collectorId;
    private int score;
    private String comment;

    /** Null on ratings saved before sewage ratings existed - treat as COLLECTION. */
    private RequestType requestType;

    @CreatedDate
    private Instant createdAt;
}