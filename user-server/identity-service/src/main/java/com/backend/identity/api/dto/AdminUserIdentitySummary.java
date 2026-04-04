package com.backend.identity.api.dto;

import java.util.List;
import java.util.UUID;

public record AdminUserIdentitySummary(
        UUID id,
        String role,
        List<String> keycloakRoles,
        String status,
        boolean enabled,
        boolean emailVerified) {
}
