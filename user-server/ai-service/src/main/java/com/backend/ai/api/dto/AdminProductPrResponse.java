package com.backend.ai.api.dto;

import java.util.List;

public record AdminProductPrResponse(
        String title,
        String excerpt,
        String caption,
        String contentHtml,
        List<String> suggestedTags,
        String disclaimer,
        boolean llmBacked,
        String model) {
}
