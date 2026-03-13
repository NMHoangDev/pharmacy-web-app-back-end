package com.backend.appointment.client.dto.pos;

import java.util.List;

public record PosProductSearchPageDto(
        List<PosProductSearchItemDto> content,
        long totalElements,
        int page,
        int size,
        int totalPages) {
}
