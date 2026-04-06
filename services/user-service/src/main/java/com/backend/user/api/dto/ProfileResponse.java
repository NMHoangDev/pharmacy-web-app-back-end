package com.backend.user.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String email,
        String phone,
        String fullName,
        String avatarBase64,
        Instant createdAt) {
}
