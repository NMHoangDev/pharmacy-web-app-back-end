package com.backend.common.security;

import java.time.Instant;

public record AuthErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId) {
}
