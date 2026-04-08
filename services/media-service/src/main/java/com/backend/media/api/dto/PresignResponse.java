package com.backend.media.api.dto;

public record PresignResponse(String bucket, String key, String url) {
}
