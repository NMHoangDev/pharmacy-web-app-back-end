package com.backend.appointment.api.dto;

import java.util.List;

public record ConsultationPrescriptionProductPageResponse(
        List<ConsultationPrescriptionProductResponse> content,
        long totalElements,
        int page,
        int size,
        int totalPages) {
}
