package com.backend.media.api.dto;

public record Base64ImageRequest(
        String base64,
        String filename,
        String contentType) {
}
