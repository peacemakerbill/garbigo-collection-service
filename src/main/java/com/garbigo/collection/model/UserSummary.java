package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Local, read-only mirror of an auth-service user.
 *
 * <p>This is NOT this service's source of truth for identity - it's a cache,
 * kept current by {@link com.garbigo.collection.messaging.UserCreatedEventListener}
 * consuming auth-service's {@code user-created} event. {@code id} mirrors
 * the auth-service user id directly rather than being locally generated.
 *
 * <p>{@code role} is left as a plain String for now - auth-service's role
 * representation isn't part of this service's decided domain model yet.
 * It's also what {@link com.garbigo.collection.security.JwtFilter} reads to
 * grant role-based authorities, since the JWT itself carries no role claim -
 * see that class for the staleness this implies.
 *
 * <p>TODO: scaffolding placeholder - no validation or indexes added yet.
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
