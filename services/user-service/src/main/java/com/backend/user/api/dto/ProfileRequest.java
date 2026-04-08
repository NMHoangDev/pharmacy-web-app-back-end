package com.backend.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfileRequest(@Email @NotBlank String email,
                String phone,
                String fullName,
                String avatarBase64) {
}
