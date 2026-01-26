package com.backend.consultation.api.dto;

import java.time.LocalDateTime;

public record AvailableSlotResponse(LocalDateTime startAt, LocalDateTime endAt, boolean available) {
}
