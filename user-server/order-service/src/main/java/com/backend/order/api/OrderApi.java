package com.backend.order.api;

import com.backend.order.api.dto.*;
import com.backend.order.service.OrderService;
import com.backend.order.service.orderdetail.OrderDetailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({ "/api/order", "/api/orders" })
public class OrderApi {

    private final OrderService orderService;
    private final OrderDetailService orderDetailService;

    public OrderApi(OrderService orderService, OrderDetailService orderDetailService) {
        this.orderService = orderService;
        this.orderDetailService = orderDetailService;
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
    public ResponseEntity<OrderDetailResponse> getOrder(
            @PathVariable UUID orderId,
            Authentication authentication) {
        Jwt jwt = extractJwt(authentication);
        return ResponseEntity.ok(orderDetailService.getMyOrderDetail(orderId, jwt, authentication));
    }

    @GetMapping("/my-orders/{orderId}")
    public ResponseEntity<OrderDetailResponse> getMyOrder(
            @PathVariable UUID orderId,
            Authentication authentication) {
        Jwt jwt = extractJwt(authentication);
        return ResponseEntity.ok(orderDetailService.getMyOrderDetail(orderId, jwt, authentication));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> listByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(orderService.listByUser(userId));
    }

    private Jwt extractJwt(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }
        return null;
    }
}
