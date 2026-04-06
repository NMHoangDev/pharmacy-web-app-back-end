package com.backend.content.api.dto;

import java.util.UUID;

public record RelatedPostItem(UUID id, String slug, String title) {
}
