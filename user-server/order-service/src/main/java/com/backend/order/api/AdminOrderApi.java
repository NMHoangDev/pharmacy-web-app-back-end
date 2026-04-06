package com.backend.order.api;

import com.backend.order.api.dto.OrderResponse;
import com.backend.order.api.dto.UserOrderCountResponse;
import com.backend.order.api.dto.AssignBranchRequest;
import com.backend.order.api.dto.BranchAvailabilityResponse;
import com.backend.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderApi {

    private final OrderService orderService;

    public AdminOrderApi(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(@RequestParam(required = false) String status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID branchId) {
        return ResponseEntity.ok(orderService.listAll(userId, status, branchId));
    }

    @GetMapping("/user-counts")
    public ResponseEntity<List<UserOrderCountResponse>> countOrdersByUser() {
        return ResponseEntity.ok(orderService.countPlacedOrdersByUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) @Valid Map<String, String> body) {
        String resolvedStatus = status;
        if ((resolvedStatus == null || resolvedStatus.isBlank()) && body != null) {
            resolvedStatus = body.getOrDefault("status", "");
        }
        return ResponseEntity.ok(orderService.updateOrderStatus(id, resolvedStatus));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @GetMapping("/{id}/branch-availability")
    public ResponseEntity<BranchAvailabilityResponse> branchAvailability(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getBranchAvailability(id));
    }

    @PostMapping("/{id}/assign-branch")
    public ResponseEntity<OrderResponse> assignBranch(@PathVariable UUID id,
            @RequestBody @Valid AssignBranchRequest request) {
        return ResponseEntity.ok(orderService.assignBranch(id, request));
    }
}
