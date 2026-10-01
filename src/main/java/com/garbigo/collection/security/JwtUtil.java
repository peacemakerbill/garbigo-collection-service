package com.garbigo.collection.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Validates JWTs issued by auth-service - this service never issues tokens
 * itself. jwt.secret is Base64-encoded, matching auth-service's own
 * JwtUtil.getSignInKey() exactly - treating it as a plain UTF-8 string
 * instead (an earlier version of this file did) derives a completely
 * different signing key from the same configured value, and every
 * signature check fails silently as a result.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

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