package com.backend.branch.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record BranchHolidayRequest(
        @NotNull LocalDate date,
        @NotBlank String type,
        LocalTime openTime,
        LocalTime closeTime,
        String note) {
}
