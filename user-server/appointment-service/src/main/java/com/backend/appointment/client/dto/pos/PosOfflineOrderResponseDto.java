package com.backend.appointment.client.dto.pos;

import java.util.UUID;

public record PosOfflineOrderResponseDto(
        UUID id,
        String orderCode,
        String consultationId,
        Long totalAmount,
        String status) {
}
