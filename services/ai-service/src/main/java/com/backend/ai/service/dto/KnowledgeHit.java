package com.backend.ai.service.dto;

import com.backend.ai.api.dto.ChatSourceDto;

public record KnowledgeHit(
        String title,
        String genericName,
        String snippet,
        String source,
        String recordType,
        String topic,
        String audience,
        String riskLevel,
        String warning,
        Integer score) {

    public ChatSourceDto toSource() {
        String label = title;
        if (genericName != null && !genericName.isBlank() && !genericName.equalsIgnoreCase(title)) {
            label = title + " / " + genericName;
        }
        return new ChatSourceDto(
                recordType == null || recordType.isBlank() ? "knowledge" : recordType,
                null,
                label,
                trimSnippet(snippet),
                null,
                null,
                null,
                source,
                topic,
                audience,
                riskLevel,
                warning);
    }

    private static String trimSnippet(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() <= 280) {
            return normalized;
        }
        return normalized.substring(0, 277) + "...";
    }
}
