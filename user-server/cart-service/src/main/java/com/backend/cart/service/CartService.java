package com.backend.cart.service;

import com.backend.cart.api.dto.CartItemRequest;
import com.backend.cart.api.dto.CartResponse;
import com.backend.cart.api.dto.CheckoutRequest;
import com.backend.cart.api.dto.CheckoutResponse;
import com.backend.cart.api.dto.PaymentRequest;
import com.backend.cart.service.dto.InventoryCommitRequest;
import com.backend.cart.service.dto.InventoryItemQuantity;
import com.backend.cart.service.dto.InventoryReleaseRequest;
import com.backend.cart.service.dto.InventoryReserveRequest;
import com.backend.cart.service.dto.InventoryReserveResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.UUID;

@Service
public class CartService {

    private final RestClient orderRestClient;
    private final RestClient inventoryRestClient;

    public CartService(
            @Qualifier("orderServiceRestClient") RestClient orderServiceRestClient,
            @Qualifier("inventoryServiceRestClient") RestClient inventoryServiceRestClient) {
        this.orderRestClient = orderServiceRestClient;
        this.inventoryRestClient = inventoryServiceRestClient;
    }

    public CartResponse getCart(UUID userId) {
        try {
            CartResponse response = orderRestClient.get()
                    .uri("/api/order/cart/{userId}", userId)
                    .retrieve()
                    .body(CartResponse.class);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from order-service");
            }
            return response;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "order-service unavailable", ex);
        }
    }

    public CartResponse upsertCartItem(UUID userId, CartItemRequest request, String authorization) {
        try {
            var spec = orderRestClient.post()
                    .uri("/api/order/cart/{userId}/items", userId);

            if (authorization != null && !authorization.isBlank()) {
                spec = spec.header("Authorization", authorization);
            }

            CartResponse response = spec
                    .body(request)
                    .retrieve()
                    .body(CartResponse.class);

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from order-service");
            }
            return response;

        } catch (RestClientResponseException ex) {
            // Downstream có phản hồi HTTP lỗi (401/403/404/500...)
            HttpStatusCode sc = ex.getStatusCode();
            int code = sc.value();

            // Nếu bạn muốn log rõ:
            // log.error("order-service error status={}, body={}", code,
            // ex.getResponseBodyAsString(), ex);

            HttpStatus mapped = HttpStatus.resolve(code);
            if (mapped == null)
                mapped = HttpStatus.BAD_GATEWAY;

            throw new ResponseStatusException(mapped, "order-service returned " + code, ex);

        } catch (ResourceAccessException ex) {
            // Không connect được / timeout / DNS
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cannot reach order-service", ex);
        }
    }

    // Backward compatibility overload: allow callers to invoke the old 2-arg
    // signature
    public CartResponse upsertCartItem(UUID userId, CartItemRequest request) {
        return upsertCartItem(userId, request, null);
    }

    public CartResponse removeCartItem(UUID userId, UUID productId) {
        try {
            CartResponse response = orderRestClient.delete()
                    .uri("/api/order/cart/{userId}/items/{productId}", userId, productId)
                    .retrieve()
                    .body(CartResponse.class);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from order-service");
            }
            return response;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "order-service unavailable", ex);
        }
    }

    public CheckoutResponse checkout(CheckoutRequest request) {
        try {
            CheckoutResponse orderResponse = orderRestClient.post()
                    .uri("/api/order/checkout")
                    .body(request)
                    .retrieve()
                    .body(CheckoutResponse.class);
            if (orderResponse == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from order-service");
            }

            InventoryReserveResponse reserveResponse = reserveInventory(orderResponse.orderId(), request);
            return new CheckoutResponse(orderResponse.orderId(), orderResponse.status(),
                    reserveResponse.reservationId());
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "order-service unavailable", ex);
        }
    }

    public CheckoutResponse pay(PaymentRequest request) {
        try {
            CheckoutResponse orderResponse = orderRestClient.post()
                    .uri("/api/order/pay")
                    .body(request)
                    .retrieve()
                    .body(CheckoutResponse.class);
            if (orderResponse == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from order-service");
            }

            commitInventory(request.reservationId(), request.orderId());
            return orderResponse;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "order-service unavailable", ex);
        }
    }

    private InventoryReserveResponse reserveInventory(UUID orderId, CheckoutRequest request) {
        try {
            var items = request.items().stream()
                    .map(i -> new InventoryItemQuantity(i.productId(), i.quantity()))
                    .toList();
            InventoryReserveRequest reserveRequest = new InventoryReserveRequest(orderId, items, 900);
            InventoryReserveResponse response = inventoryRestClient.post()
                    .uri("/api/inventory/internal/inventory/reserve")
                    .body(reserveRequest)
                    .retrieve()
                    .body(InventoryReserveResponse.class);
            if (response == null || response.reservationId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from inventory-service");
            }
            return response;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "inventory-service unavailable", ex);
        }
    }

    private void commitInventory(UUID reservationId, UUID orderId) {
        try {
            InventoryCommitRequest commitRequest = new InventoryCommitRequest(reservationId, orderId);
            inventoryRestClient.post()
                    .uri("/api/inventory/internal/inventory/commit")
                    .body(commitRequest)
                    .retrieve()
                    .body(Void.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "inventory-service unavailable", ex);
        }
    }

    public void releaseInventory(UUID reservationId, UUID orderId) {
        try {
            InventoryReleaseRequest releaseRequest = new InventoryReleaseRequest(reservationId, orderId);
            inventoryRestClient.post()
                    .uri("/api/inventory/internal/inventory/release")
                    .body(releaseRequest)
                    .retrieve()
                    .body(Void.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "inventory-service unavailable", ex);
        }
    }
}
