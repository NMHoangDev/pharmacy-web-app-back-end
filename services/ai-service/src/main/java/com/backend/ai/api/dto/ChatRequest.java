package com.backend.ai.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ChatRequest(
        UUID conversationId,
        UUID productId,
        UUID branchId,
        @Valid @NotEmpty List<ChatTurnRequest> messages) {
}
