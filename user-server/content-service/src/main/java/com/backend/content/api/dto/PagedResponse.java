package com.backend.content.api.dto;

import java.util.List;

public record PagedResponse<T>(List<T> items, Pagination pagination) {
}
