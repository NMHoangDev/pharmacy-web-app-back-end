package com.backend.inventory.api.dto;

import com.backend.inventory.model.StockDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record StockDocumentCreateRequest(
        @NotNull StockDocumentType type,
        String supplierName,
        String supplierId,
        String invoiceNo,
        String reason,
        String createdBy,
        UUID branchId,
        @NotEmpty @Valid List<StockDocumentLineRequest> lines) {
}
