package com.backend.notification.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID getActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String subject = jwtAuth.getToken().getSubject();
            if (subject != null && !subject.isBlank()) {
                return UUID.fromString(subject);
            }
        }
        return null;
    }

    public static Set<String> getRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(SecurityUtils::normalizeRole)
                .collect(Collectors.toSet());
    }

    public static boolean hasRole(String role) {
        return getRoles().contains(normalizeRole(role));
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            return normalized.substring("ROLE_".length());
        }
        return normalized;
    }
}