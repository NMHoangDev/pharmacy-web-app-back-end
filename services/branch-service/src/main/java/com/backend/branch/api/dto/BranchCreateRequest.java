package com.backend.branch.api.dto;

import jakarta.validation.constraints.NotBlank;

public record BranchCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        String status,
        String addressLine,
        String ward,
        String district,
        String city,
        String province,
        String country,
        Double latitude,
        Double longitude,
        String phone,
        String email,
        String timezone,
        String notes,
        String coverImageUrl) {
}
