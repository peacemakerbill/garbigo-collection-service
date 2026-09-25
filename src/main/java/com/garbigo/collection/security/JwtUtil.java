package com.garbigo.collection.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates JWTs issued by garbigo-auth-service. This service never issues
 * tokens itself, so there is deliberately no "generate" method here.
 *
 * <p>TODO: this is a best-effort reconstruction using jjwt, not
 * auth-service's actual JwtUtil - replace with the real one once shared, so
 * the signing algorithm and claim parsing are guaranteed to match exactly.
 * Claims assumed per the integration notes: {@code sub}, {@code userId},
 * {@code jti}, {@code iat}, {@code exp} - no role or status claim.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses and validates a token's signature and expiry. Throws
     * {@link JwtException} (or a subclass) if the token is invalid or
     * expired - callers should treat any exception here as "reject the
     * request", not attempt to recover partial claims.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(Claims claims) {
        return claims.get("userId", String.class);
    }

    public String extractJti(Claims claims) {
        return claims.getId();
    }
}