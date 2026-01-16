package com.backend.adminbff.dto;

import java.util.UUID;

public record AdminUserProfile(UUID id, String email, String phone, String fullName) {
}
