package com.backend.review.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewImageResponse(UUID id, String url, String bucket, String key, Instant createdAt) {
}
