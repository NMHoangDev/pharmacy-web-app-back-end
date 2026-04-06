package com.backend.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserNotificationRequest(
        @NotBlank @Size(max = 32) String category,
        @NotBlank @Size(max = 128) String title,
        @NotBlank @Size(max = 1024) String message,
        @Size(max = 64) String sourceType,
        @Size(max = 128) String sourceId,
        @Size(max = 64) String sourceEventType,
        @Size(max = 512) String actionUrl,
        @Size(max = 64) String createdAt) {
}
