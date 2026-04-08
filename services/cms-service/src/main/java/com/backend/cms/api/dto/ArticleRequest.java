package com.backend.cms.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ArticleRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String slug,
        @NotBlank @Size(max = 8192) String body,
        @Size(max = 255) String author,
        LocalDateTime publishedAt,
        Boolean featured,
        Boolean enabled) {
}
