package com.backend.inventory.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdjustRequest(@NotNull UUID productId,
        @NotNull Integer delta,
        String reason) {
}
