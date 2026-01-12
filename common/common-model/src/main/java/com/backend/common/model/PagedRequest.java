package com.backend.common.model;

import java.util.List;

/**
 * Lightweight paging request used across services.
 */
public record PagedRequest(int page, int size, List<String> sort) {
    public PagedRequest {
        page = Math.max(0, page);
        size = size <= 0 ? 20 : size;
        sort = sort == null ? List.of() : List.copyOf(sort);
    }
}
