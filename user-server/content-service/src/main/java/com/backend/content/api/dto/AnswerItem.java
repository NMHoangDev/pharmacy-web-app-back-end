package com.backend.content.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AnswerItem(
        UUID id,
        String content,
        UserSummaryDto author,
        boolean isPinned,
        boolean isBestAnswer,
        Instant createdAt) {
}
