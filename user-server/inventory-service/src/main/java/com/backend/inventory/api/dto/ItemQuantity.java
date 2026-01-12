package com.backend.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ItemQuantity(@NotNull UUID productId, @Min(1) int qty) {
}
