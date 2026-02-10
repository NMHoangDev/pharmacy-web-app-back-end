package com.backend.inventory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BranchClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final long ttlSeconds;
    private final Map<UUID, CacheEntry<BranchInternalDto>> branchCache = new ConcurrentHashMap<>();
    private final Map<UUID, CacheEntry<BranchSettingsDto>> settingsCache = new ConcurrentHashMap<>();

    public BranchClient(RestTemplate restTemplate,
            @Value("${inventory.branch-service.base-url:http://localhost:7030}") String baseUrl,
            @Value("${inventory.branch-service.cache-ttl-seconds:60}") long ttlSeconds) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.ttlSeconds = ttlSeconds;
    }

    public BranchInternalDto getBranch(UUID branchId) {
        CacheEntry<BranchInternalDto> cached = branchCache.get(branchId);
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }
        BranchInternalDto dto;
        try {
            ResponseEntity<BranchInternalDto> response = restTemplate.getForEntity(
                    baseUrl + "/internal/branches/" + branchId, BranchInternalDto.class);
            dto = response.getBody();
        } catch (Exception ex) {
            return null;
        }
        if (dto != null) {
            branchCache.put(branchId, new CacheEntry<>(dto, ttlSeconds));
        }
        return dto;
    }

    public BranchSettingsDto getSettings(UUID branchId) {
        CacheEntry<BranchSettingsDto> cached = settingsCache.get(branchId);
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }
        BranchSettingsDto dto;
        try {
            ResponseEntity<BranchSettingsDto> response = restTemplate.getForEntity(
                    baseUrl + "/internal/branches/" + branchId + "/settings", BranchSettingsDto.class);
            dto = response.getBody();
        } catch (Exception ex) {
            return null;
        }
        if (dto != null) {
            settingsCache.put(branchId, new CacheEntry<>(dto, ttlSeconds));
        }
        return dto;
    }

    private record CacheEntry<T>(T value, long ttlSeconds, Instant createdAt) {
        CacheEntry(T value, long ttlSeconds) {
            this(value, ttlSeconds, Instant.now());
        }

        boolean isExpired() {
            return createdAt.plusSeconds(ttlSeconds).isBefore(Instant.now());
        }
    }

    public record BranchInternalDto(UUID id, String code, String name, String status, String timezone) {
    }

    public record BranchSettingsDto(UUID branchId, int slotDurationMinutes, int bufferBeforeMinutes,
            int bufferAfterMinutes, int leadTimeMinutes, String cutoffTime,
            Integer maxBookingsPerPharmacistPerDay, Integer maxBookingsPerCustomerPerWeek,
            String channelsJson, Boolean pickupEnabled, Boolean deliveryEnabled,
            String deliveryZonesJson, String shippingFeeRulesJson, String defaultWarehouseCode,
            Boolean allowNegativeStock, Integer defaultReorderPoint, Boolean enableFefo) {
    }
}
