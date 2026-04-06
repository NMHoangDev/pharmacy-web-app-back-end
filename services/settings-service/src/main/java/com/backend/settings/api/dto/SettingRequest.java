package com.backend.settings.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettingRequest(
        @NotBlank @Size(max = 255) String scope,
        @NotBlank @Size(max = 255) String key,
        @NotBlank @Size(max = 2048) String value,
        @Size(max = 1024) String description,
        Boolean secure) {
}
