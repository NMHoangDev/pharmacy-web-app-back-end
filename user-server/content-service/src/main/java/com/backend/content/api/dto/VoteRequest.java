package com.backend.content.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record VoteRequest(
        @Min(-1) @Max(1) int value) {
}
