package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedLocationResponse {

    private String id;
    private String label;
    private LocationResponse location;
    private Instant createdAt;
}