package com.backend.branch.api.dto;

import java.time.LocalTime;
import java.util.UUID;

public record BranchSettingsSummaryResponse(
                UUID branchId,
                int slotDurationMinutes,
                int bufferBeforeMinutes,
                int bufferAfterMinutes,
                int leadTimeMinutes,
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
