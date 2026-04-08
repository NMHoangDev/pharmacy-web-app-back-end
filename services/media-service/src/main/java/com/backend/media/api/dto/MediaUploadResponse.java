package com.backend.media.api.dto;

public record MediaUploadResponse(String bucket, String key, String presignedUrl) {
}
