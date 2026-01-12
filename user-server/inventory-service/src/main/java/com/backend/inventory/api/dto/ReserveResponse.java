package com.backend.inventory.api.dto;

import java.util.UUID;

public record ReserveResponse(UUID reservationId, String status) {
}
