package com.garbigo.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Best-effort guess at the shape of auth-service's GET /users/live-location/{id}
 * response - never confirmed, unlike /internal/users. Also unconfirmed:
 * whether that endpoint even accepts the internal API key the way
 * /internal/users does, or whether it's JWT-only (its Postman folder
 * doesn't group it with the other internal-key-protected routes).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveLocationResponse {

    private Double latitude;
    private Double longitude;
    private Instant recordedAt;
}