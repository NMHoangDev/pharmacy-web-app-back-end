package com.backend.appointment.client.dto.pos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PosCreateOfflineOrderRequestDto(
        @NotNull UUID branchId,
        @NotNull UUID pharmacistId,
        @NotEmpty List<@Valid PosOrderItemRequestDto> items,
        @Min(0) Long discount,
        @Min(0) Long taxFee,
        String customerName,
        String customerPhone,
        String note,
        String consultationId) {
}
