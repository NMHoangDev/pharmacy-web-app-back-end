package com.backend.pharmacist.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PharmacistResponse(
        UUID id,
        String code,
        String name,
        String email,
        String phone,
        String avatarUrl,
        String specialty,
        int experienceYears,
        String status,
        boolean verified,
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
        UUID branchId,
        String branchName,
        Instant createdAt,
        Instant updatedAt) {
}
