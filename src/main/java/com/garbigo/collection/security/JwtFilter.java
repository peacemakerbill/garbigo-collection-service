package com.garbigo.collection.security;

import com.garbigo.collection.model.UserSummary;
import com.garbigo.collection.service.UserSummaryService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
 * Validates the incoming JWT (signature + expiry via {@link JwtUtil}, plus
 * revocation via the shared Redis denylist) and, if valid, authenticates the
 * request with the caller's userId as principal.
 *
 * <p>The JWT itself carries no role claim (see the auth-service integration
 * notes in the project README), so role-based {@code @PreAuthorize} checks
 * depend on a lookup into the local {@link UserSummary} cache here. That
 * cache is only as fresh as the last {@code user-created} event this
 * service consumed - if auth-service changes a user's role after account
 * creation, this service won't know until that gap is closed on
 * auth-service's side (a new event type). A request from a user not yet in
 * the cache is still authenticated, but granted no role authorities, so any
 * {@code @PreAuthorize} role check on it will deny rather than fail open.
 *
 * <p>TODO: this is a best-effort reconstruction, not auth-service's actual
 * JwtFilter - replace with the real one once shared, particularly to
 * confirm the exact 401 response shape expected by clients (this version
 * leaves that to Spring Security's default handling rather than writing a
 * custom response body).
 */
@Component
@RequiredArgsConstructor
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
                // If revoked, we simply don't authenticate here - the
                // downstream .authenticated()/@PreAuthorize checks reject
                // the unauthenticated request rather than this filter
                // writing the response directly.
            } catch (JwtException | IllegalArgumentException e) {
                // Invalid/expired token - leave the SecurityContext empty
                // and let Spring Security's normal unauthenticated handling
                // take over.
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
        List<GrantedAuthority> authorities = userSummaryService.findById(userId)
                .map(UserSummary::getRole)
                .<List<GrantedAuthority>>map(role -> List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())))
                .orElse(List.of());

        var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
