package com.backend.order.api;

import com.backend.order.api.dto.CheckoutQuoteResponse;
import com.backend.order.api.dto.CheckoutRequest;
import com.backend.order.api.dto.CheckoutResponse;
import com.backend.order.api.dto.ShippingMethodResponse;
import com.backend.order.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @GetMapping("/checkout/shipping-methods")
    public ResponseEntity<List<ShippingMethodResponse>> getShippingMethods() {
        return ResponseEntity.ok(checkoutService.getShippingMethods());
    }

    @PostMapping("/checkout/quote")
    public ResponseEntity<CheckoutQuoteResponse> getQuote(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(checkoutService.computeQuote(request));
    }

    @PostMapping("/orders")
    public ResponseEntity<CheckoutResponse> placeOrder(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(checkoutService.placeOrder(request));
    }
}
