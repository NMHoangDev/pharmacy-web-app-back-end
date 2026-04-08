package com.backend.ai.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationTurnDto(
        UUID id,
        String role,
        String content,
        Instant createdAt) {
}
