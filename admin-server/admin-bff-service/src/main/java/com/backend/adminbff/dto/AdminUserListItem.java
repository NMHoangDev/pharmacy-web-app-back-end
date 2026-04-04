package com.backend.adminbff.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserListItem(
        UUID id,
        String email,
        String phone,
        String name,
        String fullName,
        String avatarBase64,
        Instant createdAt,
        long orderCount,
        String role,
        String status,
        List<String> keycloakRoles,
        boolean enabled,
        boolean emailVerified) {
}
