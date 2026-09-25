package com.garbigo.collection.config;

import com.garbigo.collection.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security setup for this service. Unlike auth-service, this service never
 * issues tokens or handles credentials directly - it only validates JWTs
 * (see {@link JwtFilter} / {@link com.garbigo.collection.security.JwtUtil})
 * and enforces role checks via {@code @PreAuthorize} on controller methods
 * ({@code @EnableMethodSecurity} below is what makes those annotations take
 * effect).
 *
 * <p>TODO: scaffolding-level SecurityConfig, not adapted from auth-service's
 * actual one yet - port over any auth-service-specific conventions (custom
 * entry points, CORS setup, etc.) once that file is shared.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
