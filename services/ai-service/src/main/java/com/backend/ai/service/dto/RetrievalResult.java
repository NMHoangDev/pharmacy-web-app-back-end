package com.backend.ai.service.dto;

import com.backend.ai.api.dto.ChatSourceDto;

import java.util.List;

public record RetrievalResult(
        String normalizedQuery,
        boolean exactProductRequested,
        boolean productFound,
        boolean anyInStock,
        List<RetrievedProduct> products,
        List<KnowledgeHit> knowledgeHits) {

    public List<ChatSourceDto> toSources() {
        java.util.List<ChatSourceDto> sources = new java.util.ArrayList<>();
        sources.addAll(products.stream().map(RetrievedProduct::toSource).toList());
        sources.addAll(knowledgeHits.stream().map(KnowledgeHit::toSource).toList());
        return sources;
    }
}
