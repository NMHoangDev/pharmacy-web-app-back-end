package com.backend.pharmacist.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class PosInventoryClient {

    private static final Logger log = LoggerFactory.getLogger(PosInventoryClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PosInventoryClient(RestTemplate restTemplate,
            @Value("${pos.inventory-service.base-url:http://localhost:7018}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public AvailabilityBatchResponse availabilityBatch(AvailabilityBatchRequest request) {
        try {
            ResponseEntity<AvailabilityBatchResponse> response = restTemplate.postForEntity(
                    baseUrl + "/api/inventory/internal/inventory/availability/batch",
                    request,
                    AvailabilityBatchResponse.class);
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("Inventory availability batch failed, fallback to empty stock. request={}", request, ex);
            return new AvailabilityBatchResponse(List.of());
        }
    }

    public ReserveResponse reserve(ReserveRequest request) {
        ResponseEntity<ReserveResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/inventory/internal/inventory/reserve",
                request,
                ReserveResponse.class);
        return response.getBody();
    }

    public ReserveResponse commit(CommitRequest request) {
        ResponseEntity<ReserveResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/inventory/internal/inventory/commit",
                request,
                ReserveResponse.class);
        return response.getBody();
    }

    public ReserveResponse release(ReleaseRequest request) {
        ResponseEntity<ReserveResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/inventory/internal/inventory/release",
                request,
                ReserveResponse.class);
        return response.getBody();
    }

    public void restock(RestockRequest request) {
        restTemplate.postForEntity(
                baseUrl + "/api/inventory/internal/inventory/adjust",
                new AdjustRequest(request.productId(), request.qty(), request.reason(), request.actor(),
                        "OFFLINE_ORDER", request.orderId(), request.branchId(), request.batchNo(),
                        request.expiryDate()),
                Object.class);
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

    public record CommitRequest(UUID reservationId, UUID orderId, String reason, String actor, UUID branchId) {
    }

    public record ReleaseRequest(UUID reservationId, UUID orderId, String reason, String actor, UUID branchId) {
    }

    public record AdjustRequest(UUID productId, Integer delta, String reason, String actor, String refType, UUID refId,
            UUID branchId, String batchNo, LocalDate expiryDate) {
    }

    public record RestockRequest(UUID orderId, UUID branchId, UUID productId, Integer qty, String batchNo,
            LocalDate expiryDate,
            String reason, String actor) {
    }
}
