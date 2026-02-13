package com.backend.content.security;

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

    public static String getDisplayName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String name = jwtAuth.getToken().getClaimAsString("name");
            if (name == null || name.isBlank()) {
                name = jwtAuth.getToken().getClaimAsString("preferred_username");
            }
            return name;
        }
        return null;
    }

    public static String getPrimaryRole() {
        Set<String> roles = getRoles();
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles.contains("MOD")) {
            return "MOD";
        }
        if (roles.contains("PHARMACIST")) {
            return "PHARMACIST";
        }
        if (roles.contains("USER")) {
            return "USER";
        }
        return roles.stream().findFirst().orElse(null);
    }

    public static boolean hasRole(String role) {
        String target = normalizeRole(role);
        return getRoles().contains(target);
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
