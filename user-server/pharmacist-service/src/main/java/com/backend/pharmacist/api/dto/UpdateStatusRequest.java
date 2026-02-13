package com.backend.pharmacist.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStatusRequest(@NotBlank String status, String availability) {
}
