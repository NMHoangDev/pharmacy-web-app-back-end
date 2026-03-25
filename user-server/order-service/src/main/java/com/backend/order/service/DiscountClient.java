package com.backend.order.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DiscountClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public DiscountClient(RestTemplate restTemplate,
            @Value("${order.discount-service.base-url:http://localhost:7035}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public DiscountApplyResponse validate(UUID userId, String code, String orderId, BigDecimal subtotal,
            BigDecimal shippingFee, BigDecimal total) {
        InternalApplyDiscountRequest req = new InternalApplyDiscountRequest(
                userId == null ? null : userId.toString(),
                code,
                orderId,
                new InternalApplyDiscountRequest.Order(subtotal, shippingFee, total));

        ResponseEntity<DiscountApplyResponse> response = restTemplate.postForEntity(
                baseUrl + "/internal/discounts/validate",
                req,
                DiscountApplyResponse.class);
        return response.getBody();
    }

    public DiscountApplyResponse apply(UUID userId, String code, String orderId, BigDecimal subtotal,
            BigDecimal shippingFee, BigDecimal total) {
        InternalApplyDiscountRequest req = new InternalApplyDiscountRequest(
                userId == null ? null : userId.toString(),
                code,
                orderId,
                new InternalApplyDiscountRequest.Order(subtotal, shippingFee, total));

        ResponseEntity<DiscountApplyResponse> response = restTemplate.postForEntity(
                baseUrl + "/internal/discounts/apply",
                req,
                DiscountApplyResponse.class);
        return response.getBody();
    }

    public record InternalApplyDiscountRequest(
            @NotBlank String userId,
            @NotBlank String code,
            @NotBlank String orderId,
            @NotNull @Valid Order order) {
        public record Order(@NotNull BigDecimal subtotal, @NotNull BigDecimal shippingFee, @NotNull BigDecimal total) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiscountApplyResponse(
            Boolean valid,
            String reason,
            BigDecimal discountAmount,
            BigDecimal shippingDiscount,
            BigDecimal finalTotal) {
    }
}
