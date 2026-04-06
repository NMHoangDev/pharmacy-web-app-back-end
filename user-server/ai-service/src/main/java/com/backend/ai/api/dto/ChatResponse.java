package com.backend.ai.api.dto;

import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID conversationId,
        String reply,
        boolean needsHumanSupport,
        String disclaimer,
        List<ChatSourceDto> sources) {
}
