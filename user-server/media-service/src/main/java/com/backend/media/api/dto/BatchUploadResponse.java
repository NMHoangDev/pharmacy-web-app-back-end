package com.backend.media.api.dto;

import java.util.List;

public record BatchUploadResponse(
        String albumId,
        List<MediaUploadResponse> items) {
}
