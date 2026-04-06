package com.backend.ai.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationId,
        UUID userId,
        Instant createdAt,
        Instant updatedAt,
        List<ConversationTurnDto> messages) {
}
