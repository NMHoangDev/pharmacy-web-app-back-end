package com.backend.notification.api.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String category,
        String title,
        String message,
        String sourceType,
        String sourceId,
        String sourceEventType,
        String actionUrl,
        Instant createdAt,
        boolean read) {
}