package com.backend.content.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostListItem(
        UUID id,
        String slug,
        String title,
        String excerpt,
        String coverImageUrl,
        int readingMinutes,
        List<TagDto> tags,
        UserSummaryDto author,
        String moderationStatus,
        Instant publishedAt,
        long views) {
}
