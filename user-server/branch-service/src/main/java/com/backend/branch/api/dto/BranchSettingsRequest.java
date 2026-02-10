package com.backend.branch.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record BranchSettingsRequest(
        @NotNull Integer slotDurationMinutes,
        @Min(0) Integer bufferBeforeMinutes,
        @Min(0) Integer bufferAfterMinutes,
        @Min(0) Integer leadTimeMinutes,
        LocalTime cutoffTime,
        Integer maxBookingsPerPharmacistPerDay,
        Integer maxBookingsPerCustomerPerWeek,
        String channelsJson,
        String pricingJson,
        Boolean pickupEnabled,
        Boolean deliveryEnabled,
        String deliveryZonesJson,
        String shippingFeeRulesJson,
        String defaultWarehouseCode,
        Boolean allowNegativeStock,
        Integer defaultReorderPoint,
        Boolean enableFefo) {
}
