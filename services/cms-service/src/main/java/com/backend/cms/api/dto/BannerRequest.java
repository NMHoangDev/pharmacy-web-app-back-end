package com.backend.cms.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BannerRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2048) String imageUrl,
        @Size(max = 2048) String targetUrl,
        LocalDateTime activeFrom,
        LocalDateTime activeTo,
        Integer priority,
        Boolean enabled) {
}
