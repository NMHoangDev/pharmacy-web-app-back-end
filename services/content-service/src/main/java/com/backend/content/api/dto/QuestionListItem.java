package com.backend.content.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionListItem(
        UUID id,
        String title,
        String slug,
        String excerpt,
        List<TagDto> tags,
        UserSummaryDto asker,
        String threadStatus,
        String moderationStatus,
        int answerCount,
        boolean hasPharmacistAnswer,
        Instant createdAt,
        Instant lastActivityAt) {
}
