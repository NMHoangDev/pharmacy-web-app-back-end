package com.backend.user.api.dto;

public record HealthProfileResponse(String bloodType,
        String allergies,
        String chronicConditions,
        String medications,
        String notes) {
}
