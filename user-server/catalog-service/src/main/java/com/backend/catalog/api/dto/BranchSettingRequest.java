package com.backend.catalog.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record BranchSettingRequest(
        @NotNull UUID branchId,
        BigDecimal priceOverride,
        String status,
        String note) {
}
