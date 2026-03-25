package com.backend.appointment.security;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID getActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String sub = jwtAuth.getToken().getSubject();
            if (sub != null && !sub.isBlank()) {
                return UUID.fromString(sub);
            }
        }
        return null;
    }

    public static String getActorEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String email = jwtAuth.getToken().getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) {
                return username;
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
        String target = normalizeRole(role);
        return getRoles().contains(target);
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public static boolean isPharmacist() {
        return hasRole("PHARMACIST") || hasRole("STAFF");
    }

    public static boolean isUser() {
        return hasRole("USER");
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String r = role.toUpperCase();
        if (r.startsWith("ROLE_")) {
            return r.substring("ROLE_".length());
        }
        return r;
    }
}