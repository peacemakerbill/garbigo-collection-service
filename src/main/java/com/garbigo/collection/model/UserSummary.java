package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Read-only mirror of an auth-service user, kept current by
 * UserCreatedEventListener and UserSummaryService's directory-refresh
 * fallback. Deliberately only carries what this service actually uses -
 * auth-service's real user also has phoneNumber/homeAddress/followers/
 * reviews/etc., which just get silently dropped here rather than mapped.
 */
@Document(collection = "user_summaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {

    @Id
    private String id;

    private String username;
    private String firstName;
    private String email;
    private String role;
    private boolean active;
    private boolean archived;

    /** First name if we have it, falling back to username - for greetings/display. */
    public String preferredName() {
        return (firstName != null && !firstName.isBlank()) ? firstName : username;
    }
}