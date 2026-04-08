package com.backend.inventory.api.dto;

import com.backend.inventory.model.StockDocumentStatus;
import com.backend.inventory.model.StockDocumentType;

import java.time.Instant;
import java.util.UUID;

public record StockDocumentSummaryResponse(
        UUID id,
        StockDocumentType type,
        StockDocumentStatus status,
        String supplierName,
        String invoiceNo,
        String createdBy,
        String approvedBy,
        UUID branchId,
        Instant createdAt,
        Instant submittedAt,
        Instant approvedAt,
        Instant updatedAt,
        int lineCount) {
}
