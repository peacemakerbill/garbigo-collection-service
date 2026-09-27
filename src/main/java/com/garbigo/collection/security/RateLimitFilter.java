package com.garbigo.collection.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Fixed-window rate limiter, one counter per client IP per minute, backed by
 * Redis (same instance already used for the JWT denylist) so it's correct
 * across multiple instances rather than an in-memory counter per pod.
 *
 * Runs before JwtFilter, so it keys by IP rather than the authenticated
 * user - the SecurityContext isn't populated yet at this point. Doesn't
 * special-case X-Forwarded-For, so behind a reverse proxy every request
 * would share one IP unless that's configured separately.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String TOO_MANY_REQUESTS_BODY =
            "{\"message\":\"Too many requests. Please slow down and try again shortly.\"}";

    private final StringRedisTemplate redisTemplate;
    private final int requestsPerMinute;

    public RateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${rate-limit.requests-per-minute}") int requestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String key = "rate-limit:" + request.getRemoteAddr() + ":" + (Instant.now().getEpochSecond() / 60);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(2));
        }

        if (count != null && count > requestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(TOO_MANY_REQUESTS_BODY);
            return;
        }

        filterChain.doFilter(request, response);
    }
}