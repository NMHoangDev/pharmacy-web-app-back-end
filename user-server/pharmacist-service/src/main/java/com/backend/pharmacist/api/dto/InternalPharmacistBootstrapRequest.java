package com.backend.pharmacist.api.dto;

public record InternalPharmacistBootstrapRequest(
        String name,
        String email,
        String phone,
        String avatarUrl,
        String specialty) {
}
