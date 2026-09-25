package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * An issue reported against a specific {@link CollectionRequest}.
 *
 * <p>TODO: scaffolding placeholder - no validation or indexes added yet.
 */
@Document(collection = "complaints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    private String id;

    private String collectionRequestId;

    private String reporterId;

    private String description;

    private ComplaintStatus status;

    @CreatedDate
    private Instant createdAt;
}
