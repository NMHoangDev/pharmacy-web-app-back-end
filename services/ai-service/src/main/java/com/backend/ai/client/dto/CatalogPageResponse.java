package com.backend.ai.client.dto;

import java.util.List;

public record CatalogPageResponse(List<CatalogProductDto> content) {
}
