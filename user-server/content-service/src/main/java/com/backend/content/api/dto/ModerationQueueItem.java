package com.backend.content.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ModerationQueueItem(
        String type,
        UUID id,
        String title,
        String excerpt,
        Instant createdAt,
        String moderationStatus) {
}
