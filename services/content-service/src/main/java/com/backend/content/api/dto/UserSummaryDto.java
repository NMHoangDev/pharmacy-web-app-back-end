package com.backend.content.api.dto;

import java.util.UUID;

public record UserSummaryDto(UUID id, String displayName, Boolean isAnonymous, String role) {
}
