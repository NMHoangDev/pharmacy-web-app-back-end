package com.backend.identity.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(UUID userId, String email, String phone, String fullName, String token, Instant expiresAt) {
}