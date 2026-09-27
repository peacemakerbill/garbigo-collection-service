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
    private String middleName;
    private String lastName;
    private String email;
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