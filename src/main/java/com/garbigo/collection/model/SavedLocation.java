package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A client's saved address ("Home", "Office"), referenced by id instead of re-typing a full location each booking. */
@Document(collection = "saved_locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedLocation {

    @Id
    private String id;

    private String clientId;
    private String label;
    private Location location;

    @CreatedDate
    private Instant createdAt;
}