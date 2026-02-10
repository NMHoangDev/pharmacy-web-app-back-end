package com.backend.inventory.api;

import com.backend.inventory.api.dto.*;
import com.backend.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryApi {

    private final InventoryService inventoryService;

    public InventoryApi(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("inventory-service ok");
    }

    @PostMapping("/internal/inventory/reserve")
    public ResponseEntity<ReserveResponse> reserve(@RequestBody @Valid ReserveRequest request) {
        return ResponseEntity.ok(inventoryService.reserve(request));
    }

    @PostMapping("/internal/inventory/commit")
    public ResponseEntity<ReserveResponse> commit(@RequestBody CommitRequest request) {
        return ResponseEntity.ok(inventoryService.commit(request));
    }

    @PostMapping("/internal/inventory/release")
    public ResponseEntity<ReserveResponse> release(@RequestBody ReleaseRequest request) {
        return ResponseEntity.ok(inventoryService.release(request));
    }

    @GetMapping("/internal/inventory/availability")
    public ResponseEntity<AvailabilityResponse> availability(
            @RequestParam(name = "productIds", required = false) List<UUID> productIds,
            @RequestParam(name = "productIds[]", required = false) List<UUID> productIdsAlt,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        List<UUID> resolved = (productIds == null || productIds.isEmpty()) ? productIdsAlt : productIds;
        return ResponseEntity.ok(inventoryService.availability(branchId, resolved));
    }

    @PostMapping("/internal/inventory/adjust")
    public ResponseEntity<AdjustResponse> adjust(@RequestBody @Valid AdjustRequest request) {
        return ResponseEntity.ok(inventoryService.adjust(request));
    }

    @DeleteMapping("/internal/inventory/{productId}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable UUID productId,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        boolean deleted = inventoryService.deleteItem(productId, branchId);
        return ResponseEntity.ok(Map.of("productId", productId, "deleted", deleted));
    }

    @GetMapping("/internal/inventory/activities")
    public ResponseEntity<List<InventoryActivityResponse>> listActivities(
            @RequestParam(name = "productId", required = false) UUID productId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "limit", required = false) Integer limit) {
        int safeLimit = limit == null ? 20 : limit;
        return ResponseEntity.ok(inventoryService.listActivities(productId, branchId, safeLimit));
    }
}
