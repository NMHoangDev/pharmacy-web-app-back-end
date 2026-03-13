package com.backend.review.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReviewRequest(
                @NotNull UUID productId,
                @NotNull UUID userId,
                @NotNull @Min(1) @Max(5) Integer rating,
                String title,
                @NotBlank String content,
                List<ReviewImageRequest> images) {
}
