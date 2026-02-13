package com.backend.content.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionDetailResponse(
        UUID id,
        String title,
        String content,
        QuestionContext context,
        List<TagDto> tags,
        UserSummaryDto asker,
        String threadStatus,
        String moderationStatus,
        Instant createdAt,
        PagedResponse<AnswerItem> answers) {
}
