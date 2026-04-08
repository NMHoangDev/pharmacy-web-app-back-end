package com.backend.user.api.dto;

public record HealthProfileRequest(String bloodType,
        String allergies,
        String chronicConditions,
        String medications,
        String notes) {
}
