package com.backend.adminbff.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminUserProfile(
        UUID id,
        String email,
        String phone,
        String fullName,
        String avatarBase64,
        Instant createdAt) {
}
