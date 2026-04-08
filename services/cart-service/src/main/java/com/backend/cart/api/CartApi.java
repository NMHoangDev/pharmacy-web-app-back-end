package com.backend.cart.api;

import com.backend.cart.api.dto.CartItemRequest;
import com.backend.cart.api.dto.CartResponse;
import com.backend.cart.api.dto.CheckoutRequest;
import com.backend.cart.api.dto.CheckoutResponse;
import com.backend.cart.api.dto.PaymentRequest;
import com.backend.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
public class CartApi {

    private static final Logger log = LoggerFactory.getLogger(CartApi.class);

    private final CartService cartService;

    public CartApi(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("cart-service ok");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable UUID userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponse> upsertCartItem(
            @PathVariable UUID userId,
            @RequestBody @Valid CartItemRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("upsertCartItem userId={} payload={}", userId, request);
        return ResponseEntity.ok(cartService.upsertCartItem(userId, request, authorization));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @PathVariable UUID userId,
            @PathVariable UUID productId) {
        return ResponseEntity.ok(cartService.removeCartItem(userId, productId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody @Valid CheckoutRequest request) {
        return ResponseEntity.ok(cartService.checkout(request));
    }

    @PostMapping("/pay")
    public ResponseEntity<CheckoutResponse> pay(@RequestBody @Valid PaymentRequest request) {
        return ResponseEntity.ok(cartService.pay(request));
    }
}
