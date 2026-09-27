package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private long collectionsPending;
    private long collectionsAssigned;
    private long collectionsCompleted;
    private long collectionsCancelled;
    private long complaintsOpen;
    private long complaintsResolved;
    private long registeredCollectorsCached;
}