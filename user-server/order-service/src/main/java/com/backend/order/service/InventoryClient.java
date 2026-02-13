package com.backend.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Component
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public InventoryClient(RestTemplate restTemplate,
            @Value("${order.inventory-service.base-url:http://localhost:7018}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public AvailabilityBatchResponse batchAvailability(AvailabilityBatchRequest request) {
        ResponseEntity<AvailabilityBatchResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/inventory/internal/inventory/availability/batch",
                request,
                AvailabilityBatchResponse.class);
        return response.getBody();
    }

    public ReserveResponse reserve(ReserveRequest request) {
        ResponseEntity<ReserveResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/inventory/internal/inventory/reserve",
                request,
                ReserveResponse.class);
        return response.getBody();
    }

    public record AvailabilityBatchRequest(List<UUID> branchIds, List<ItemQuantity> items) {
    }

    public record AvailabilityBatchResponse(List<AvailabilityBatchItem> items) {
    }

    public record AvailabilityBatchItem(UUID productId, List<AvailabilityByBranch> byBranch) {
    }

    public record AvailabilityByBranch(UUID branchId, int available, int onHand, int reserved) {
    }

    public record ItemQuantity(UUID productId, int qty) {
    }

    public record ReserveRequest(UUID orderId, List<ItemQuantity> items, Integer ttlSeconds, String reason,
            String actor,
            UUID branchId) {
    }

    public record ReserveResponse(UUID reservationId, String status) {
    }
}
