package com.backend.content.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostDetailResponse(
                UUID id,
                String slug,
                String title,
                String excerpt,
                String contentHtml,
                Object contentJson,
                List<TocItem> toc,
                String coverImageUrl,
                List<PostImageDto> images,
                List<TagDto> tags,
                UserSummaryDto author,
                String disclaimer,
                Instant publishedAt,
                Instant updatedAt,
                long views,
                List<RelatedPostItem> relatedPosts) {
}
