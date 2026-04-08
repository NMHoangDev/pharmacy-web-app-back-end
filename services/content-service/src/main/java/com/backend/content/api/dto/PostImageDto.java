package com.backend.content.api.dto;

import java.util.UUID;

public record PostImageDto(
        UUID id,
        String url,
        String altText,
        int position) {
}
