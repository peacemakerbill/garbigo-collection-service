package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Read-only mirror of an auth-service user, kept current by
 * {@link com.garbigo.collection.messaging.UserCreatedEventListener}. Not
 * the source of truth for identity. {@code id} is auth-service's own user
 * id. {@code role} stays a plain String until auth-service's role shape is
 * confirmed.
 */
@Document(collection = "user_summaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {

    @Id
    private String id;

    private String email;
    private String displayUsername;
    private String role;
    private boolean active;
}