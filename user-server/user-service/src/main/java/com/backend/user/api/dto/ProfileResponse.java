package com.backend.user.api.dto;

import java.util.UUID;

public record ProfileResponse(UUID id, String email, String phone, String fullName) {
}
