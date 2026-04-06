package com.backend.ai.client.dto;

import java.util.UUID;

public record BranchInternalResponse(
        UUID id,
        String code,
        String name,
        String status,
        String timezone) {
}
