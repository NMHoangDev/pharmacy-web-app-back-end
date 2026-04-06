package com.backend.ai.api.dto;

import java.util.List;

public record AdminCampaignAnalysisResponse(
        String headline,
        String summary,
        List<String> opportunities,
        List<String> risks,
        List<CampaignProposal> proposals,
        boolean llmBacked,
        String model) {

    public record CampaignProposal(
            String id,
            String title,
            String objective,
            String timing,
            String rationale,
            String offerStructure,
            String expectedImpact,
            List<String> recommendedProducts,
            MarketingContent marketing) {
    }

    public record MarketingContent(
            String banner,
            String pushNotification,
            String sms,
            String socialPost) {
    }
}
