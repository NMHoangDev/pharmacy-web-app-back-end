package com.backend.inventory.api;

import com.backend.inventory.api.dto.*;
import com.backend.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AvailabilityResponse> availability(@RequestParam(name = "productIds") List<UUID> productIds) {
        return ResponseEntity.ok(inventoryService.availability(productIds));
    }

    @PostMapping("/internal/inventory/adjust")
    public ResponseEntity<AdjustResponse> adjust(@RequestBody @Valid AdjustRequest request) {
        return ResponseEntity.ok(inventoryService.adjust(request));
    }
}
