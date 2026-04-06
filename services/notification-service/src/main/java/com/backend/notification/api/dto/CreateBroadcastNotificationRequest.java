package com.backend.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBroadcastNotificationRequest(
        @NotBlank @Size(max = 32) String category,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 32) String targetRole,
        @Size(max = 64) String sourceType,
        @Size(max = 128) String sourceId,
        @Size(max = 512) String actionUrl) {
}