package com.backend.order.api;

import com.backend.order.api.dto.*;
import com.backend.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/order")
public class OrderApi {

    private final OrderService orderService;

    public OrderApi(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("order-service ok");
    }

    // Cart endpoints
    @GetMapping("/cart/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable UUID userId) {
        return ResponseEntity.ok(orderService.getCart(userId));
    }

    @PostMapping("/cart/{userId}/items")
    public ResponseEntity<CartResponse> upsertCartItem(@PathVariable UUID userId,
            @RequestBody @Valid CartItemRequest request) {
        return ResponseEntity.ok(orderService.upsertCartItem(userId, request));
    }

    @DeleteMapping("/cart/{userId}/items/{productId}")
    public ResponseEntity<CartResponse> removeCartItem(@PathVariable UUID userId, @PathVariable UUID productId) {
        return ResponseEntity.ok(orderService.removeCartItem(userId, productId));
    }

    // Checkout
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody @Valid CheckoutRequest request) {
        return ResponseEntity.ok(orderService.checkout(request));
    }

    // Payment confirmation
    @PostMapping("/pay")
    public ResponseEntity<CheckoutResponse> pay(@RequestBody @Valid PaymentRequest request) {
        return ResponseEntity.ok(orderService.pay(request));
    }

    // Order detail
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }
}
