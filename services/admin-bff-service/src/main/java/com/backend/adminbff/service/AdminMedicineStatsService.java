package com.backend.adminbff.service;

import com.backend.adminbff.dto.stats.MedicinesStatsResponse;
import com.backend.adminbff.repository.AdminMedicineStatsRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminMedicineStatsService {

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final Logger log = LoggerFactory.getLogger(AdminMedicineStatsService.class);

    private final AdminMedicineStatsRepository repository;

    public AdminMedicineStatsService(AdminMedicineStatsRepository repository) {
        this.repository = repository;
    }

    public MedicinesStatsResponse getStats(String range) {
        List<Map<String, Object>> products = loadProducts(range);
        long total = products.size();

        long active = products.stream()
                .filter(java.util.Objects::nonNull)
                .map(this::resolveStatus)
                .filter(status -> "ACTIVE".equalsIgnoreCase(status))
                .count();

        long hiddenOrDiscontinued = products.stream()
                .filter(java.util.Objects::nonNull)
                .map(this::resolveStatus)
                .filter(status -> "HIDDEN".equalsIgnoreCase(status)
                        || "INACTIVE".equalsIgnoreCase(status)
                        || "DISCONTINUED".equalsIgnoreCase(status))
                .count();

        List<UUID> productIds = products.stream()
                .map(product -> product.get("id"))
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .map(this::safeUuid)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<UUID, Integer> availableStockByProduct = loadAvailableStockByProductIds(range, productIds);

        long outOfStock = availableStockByProduct.values().stream()
                .filter(stock -> stock <= 0)
                .count();

        long lowStock = availableStockByProduct.values().stream()
                .filter(stock -> stock > 0 && stock <= LOW_STOCK_THRESHOLD)
                .count();

        return new MedicinesStatsResponse(total, active, lowStock, outOfStock, hiddenOrDiscontinued);
    }

    private List<Map<String, Object>> loadProducts(String range) {
        try {
            return repository.findAllCatalogProducts();
        } catch (RuntimeException ex) {
            log.warn("[ADMIN MEDICINES STATS] range={} failed to load catalog products. Returning empty stats.",
                    range, ex);
            return Collections.emptyList();
        }
    }

    private Map<UUID, Integer> loadAvailableStockByProductIds(String range, List<UUID> productIds) {
        try {
            return repository.findAvailableStockByProductIds(productIds);
        } catch (RuntimeException ex) {
            log.warn("[ADMIN MEDICINES STATS] range={} failed to load inventory availability. "
                    + "Returning catalog-only stats.", range, ex);
            return Collections.emptyMap();
        }
    }

    private UUID safeUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveStatus(Map<String, Object> product) {
        Object status = product.get("effectiveStatus");
        if (status == null) {
            status = product.get("globalStatus");
        }
        if (status == null) {
            status = product.get("branchStatus");
        }
        if (status == null) {
            status = product.get("status");
        }
        return String.valueOf(status == null ? "" : status);
    }
}
