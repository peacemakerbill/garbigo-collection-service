package com.garbigo.collection.security;

import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.service.UserSummaryService;
import feign.RetryableException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates the JWT and checks the shared Redis revocation denylist. The
 * token carries no role claim, so role authorities come from the local
 * UserSummary cache via UserSummaryService.resolve() - a cache hit (the
 * common case) costs nothing extra; a miss triggers one live directory
 * refresh from auth-service right then, so a brand-new deployment or an
 * account older than this service's own uptime self-heals on its very
 * first request instead of waiting on the periodic sync or a manual
 * /users/resync call. Deliberately NOT a live call on every request:
 * this filter runs on every single request to this service, and the only
 * confirmed auth-service endpoint for this (GET /internal/users) is
 * unpaginated and bulk-only - calling it unconditionally here would mean
 * pulling the entire user directory on every API call, and would make
 * every endpoint's availability depend on auth-service's.
 *
 * This still leaves one kind of staleness on the table: once a user IS
 * cached, a LATER role change on auth-service's side isn't picked up
 * until the next cache miss, the next periodic resync, or a manual
 * /users/resync call - not truly instant for that case. If that gap
 * matters more than the request-cost tradeoff above, say so.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private static final String REVOKED_KEY_PREFIX = "revoked:jti:";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserSummaryService userSummaryService;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtUtil.parseClaims(token);
                String jti = jwtUtil.extractJti(claims);

                if (!isRevoked(jti)) {
                    authenticate(jwtUtil.extractUserId(claims));
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Deliberately logs the exception type/message, not the token
                // itself - e.g. "JwtException: JWT signature does not match
                // locally computed signature" (JWT_SECRET mismatch between
                // services) vs "ExpiredJwtException: JWT expired at ..." vs
                // "MalformedJwtException" (often an empty/unresolved token
                // variable in a REST client) - each points somewhere different.
                log.warn("JWT validation failed on {} {}: {}: {}",
                        request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_KEY_PREFIX + jti));
    }

    private void authenticate(String userId) {
        List<GrantedAuthority> authorities = resolveAuthorities(userId);
        var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    /**
     * auth-service being unreachable during a cache-miss refresh must not
     * crash request handling for this (or any other) request - this filter
     * runs on every single one. Degrades to the same "authenticated, no
     * roles yet" outcome a cache miss already produced before this class
     * called resolve() instead of findById(), rather than letting
     * RetryableException escape the filter chain uncaught.
     */
    private List<GrantedAuthority> resolveAuthorities(String userId) {
        try {
            return userSummaryService.resolve(userId)
                    .map(UserSummary::getRole)
                    .<List<GrantedAuthority>>map(role -> List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())))
                    .orElse(List.of());
        } catch (RetryableException e) {
            log.warn("auth-service unreachable while resolving role for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }
}