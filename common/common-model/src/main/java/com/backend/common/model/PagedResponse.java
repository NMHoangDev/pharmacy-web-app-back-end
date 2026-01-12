package com.backend.common.model;

import java.util.List;

/**
 * Standard paged response envelope.
 */
public record PagedResponse<T>(List<T> data, long totalElements, int page, int size, int totalPages) {
    public PagedResponse {
        data = data == null ? List.of() : List.copyOf(data);
        page = Math.max(0, page);
        size = Math.max(1, size);
        totalPages = Math.max(0, totalPages);
        totalElements = Math.max(0, totalElements);
    }
}
