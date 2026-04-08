package com.backend.pharmacist.api.dto.pos;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelOfflineOrderRequest(@NotNull UUID pharmacistId, String reason) {
}
