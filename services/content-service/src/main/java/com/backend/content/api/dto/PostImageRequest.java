package com.backend.content.api.dto;

public record PostImageRequest(
        String url,
        String altText,
        Integer position) {
}
