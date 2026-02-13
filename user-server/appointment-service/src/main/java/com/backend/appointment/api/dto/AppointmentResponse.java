package com.backend.appointment.api.dto;

import com.backend.appointment.client.dto.PharmacistPreviewDto;
import com.backend.appointment.model.AppointmentStatus;
import com.backend.appointment.model.Channel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
                UUID id,
                UUID userId,
                String fullName,
                String contact,
                UUID pharmacistId,
                UUID branchId,
                LocalDateTime startAt,
                LocalDateTime endAt,
                AppointmentStatus status,
                Channel channel,
                String notes,
                String cancelReason,
                String rescheduleReason,
                String refundReason,
                String noShowReason,
                PharmacistPreviewDto pharmacist,
                Instant createdAt,
                Instant updatedAt) {
}
