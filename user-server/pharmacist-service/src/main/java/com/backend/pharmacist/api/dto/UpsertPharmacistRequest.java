package com.backend.pharmacist.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record UpsertPharmacistRequest(
        String code,
        @NotBlank String name,
        String email,
        String phone,
        String avatarUrl,
        @NotBlank String specialty,
        @Min(0) Integer experienceYears,
        String status,
        Boolean verified,
        String availability,
        Double rating,
        Integer reviewCount,
        String bio,
        String education,
        List<String> languages,
        List<String> workingDays,
        String workingHours,
        List<String> consultationModes,
        String licenseNumber,
        UUID branchId) {
}
