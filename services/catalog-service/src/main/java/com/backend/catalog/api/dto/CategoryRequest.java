package com.backend.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CategoryRequest(
                @NotBlank String name,
                @NotBlank String slug,
                String description,
                Boolean active,
                Integer sortOrder,
                UUID parentId) {
}
