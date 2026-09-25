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
 * Validates the JWT and checks the shared Redis revocation denylist. The
 * token carries no role claim, so role authorities come from the local
 * UserSummary cache - a user not yet in that cache is authenticated but
 * gets no roles, so @PreAuthorize checks deny rather than fail open.
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
            } catch (JwtException | IllegalArgumentException e) {
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