package com.garbigo.collection.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

/** The caller's roles without the ROLE_ prefix (e.g. "CLIENT"), for service-level access checks. */
public final class CallerRoles {

    private static final String ROLE_PREFIX = "ROLE_";

    private CallerRoles() {
    }

    public static Set<String> of(Authentication authentication) {
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toSet());
    }
}