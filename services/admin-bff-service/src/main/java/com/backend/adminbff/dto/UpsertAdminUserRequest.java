package com.backend.adminbff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpsertAdminUserRequest(@Email @NotBlank String email, String phone, String fullName) {
}
