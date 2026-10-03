package com.garbigo.collection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Read-only mirror of an auth-service user, kept current by
 * UserCreatedEventListener and UserSummaryService's directory refresh.
 * Carries what this service uses plus phoneNumber and profilePictureUrl;
 * auth-service's other user fields (homeAddress, followers, reviews, ...)
 * are still dropped rather than mapped.
 *
 * phoneNumber and profilePictureUrl are only filled in if auth-service
 * actually sends them on GET /internal/users and in the user-created
 * event - they were not among the fields confirmed for either, so they
 * stay null until it does. profilePictureUrl is just the URL, never the
 * image itself. Both are personal data copied into this database.
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
    private String middleName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private String role;
    private boolean active;
    private boolean archived;

    /** Full name from whichever name parts are present, falling back to username if none are. */
    public String preferredName() {
        String fullName = Stream.of(firstName, middleName, lastName)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return fullName.isBlank() ? username : fullName;
    }
}