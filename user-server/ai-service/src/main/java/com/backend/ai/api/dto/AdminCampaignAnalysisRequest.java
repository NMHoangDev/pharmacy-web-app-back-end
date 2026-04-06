package com.backend.ai.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AdminCampaignAnalysisRequest(
        @NotBlank String range,
        UUID branchId,
        @NotBlank String branchLabel,
        @NotNull Double totalRevenue,
        @NotNull Double onlineRevenue,
        @NotNull Double posRevenue,
        @NotNull Double revenueChangePct,
        @NotNull Integer usersCount,
        @NotNull Integer appointmentsToday,
        @Valid InventorySummarySnapshot inventorySummary,
        @Valid List<ProductSnapshot> topProducts,
        @Valid List<InventoryAlertSnapshot> lowStockItems,
        @Valid ForecastSnapshot forecast) {

    public record InventorySummarySnapshot(
            @NotNull Integer skuCount,
            @NotNull Integer onHand,
            @NotNull Integer available,
            @NotNull Integer reserved,
            @NotNull Integer outOfStock) {
    }

    public record ProductSnapshot(
            @NotBlank String name,
            @NotNull Integer quantity) {
    }

    public record InventoryAlertSnapshot(
            UUID id,
            @NotBlank String name,
            String sku,
            @NotNull Integer onHand,
            @NotNull Integer threshold) {
    }

    public record ForecastSnapshot(
            @NotNull Long nextWeekRevenue,
            @NotBlank String branchLabel,
            @NotBlank String revenueNote,
            @NotBlank String channelHeadline,
            @NotBlank String channelNote,
            @Valid List<ForecastItemSnapshot> restock,
            @Valid List<ForecastItemSnapshot> stockout,
            @Valid List<ForecastItemSnapshot> slowMoving) {
    }

    public record ForecastItemSnapshot(
            UUID productId,
            @NotBlank String name,
            String badge,
            @NotBlank String note,
            @Valid List<String> meta) {
    }
}
