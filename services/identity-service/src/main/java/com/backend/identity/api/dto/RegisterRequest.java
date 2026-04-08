package com.backend.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required") @Size(max = 100, message = "Full name must be at most 100 characters") String fullName,

        @NotBlank(message = "Email is required") @Email(message = "Email is invalid") @Size(max = 150, message = "Email must be at most 150 characters") String email,

        @NotBlank(message = "Phone is required") @Size(max = 32, message = "Phone must be at most 32 characters") String phone,

        @NotBlank(message = "Password is required") @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters") String password) {
}