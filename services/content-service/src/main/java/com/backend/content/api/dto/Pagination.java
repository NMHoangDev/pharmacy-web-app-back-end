package com.backend.content.api.dto;

public record Pagination(int page, int pageSize, long total) {
}
