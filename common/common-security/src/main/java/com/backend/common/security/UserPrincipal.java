package com.backend.common.security;

import java.util.Set;

/**
 * Minimal principal representation shared between services.
 */
public record UserPrincipal(String userId, String email, String displayName, Set<String> roles) {
    public UserPrincipal {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
