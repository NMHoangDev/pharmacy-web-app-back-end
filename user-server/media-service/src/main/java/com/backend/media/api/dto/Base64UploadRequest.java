package com.backend.media.api.dto;

import java.util.List;

public record Base64UploadRequest(
        String albumId,
        List<Base64ImageRequest> images) {
}
