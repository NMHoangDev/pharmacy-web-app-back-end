package com.backend.content.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PostCreateRequest(
                @NotBlank String title,
                String slug,
                String excerpt,
                String contentHtml,
                Object contentJson,
                String coverImageUrl,
                List<PostImageRequest> images,
                List<String> tags,
                String type,
                String level,
                String topic,
                Boolean featured,
                String disclaimer,
                Integer readingMinutes) {
}
