package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A user as this service's local directory cache knows them - auth-service owns the real record. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private String id;
    private String username;
    private String name;
    private String email;
    private String role;
    private boolean active;
    private boolean archived;
}