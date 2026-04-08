package com.backend.pharmacist.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        UUID pharmacistId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String note,
        String status) {
}
