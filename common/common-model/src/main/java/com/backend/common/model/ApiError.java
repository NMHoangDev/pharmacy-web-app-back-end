package com.backend.common.model;

import java.util.Map;

/**
 * Standard API error payload.
 */
public record ApiError(ErrorCode code, String message, Map<String, String> details) {
    public ApiError {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
