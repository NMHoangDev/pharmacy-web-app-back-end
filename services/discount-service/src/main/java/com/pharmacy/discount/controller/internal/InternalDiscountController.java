package com.pharmacy.discount.controller.internal;

import com.pharmacy.discount.dto.ApplyDiscountRequest;
import com.pharmacy.discount.dto.ApplyDiscountResponse;
import com.pharmacy.discount.service.DiscountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/internal/discounts")
public class InternalDiscountController {

    private final DiscountService discountService;

    public InternalDiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping("/validate")
    public ResponseEntity<ApplyDiscountResponse> validate(@Valid @RequestBody InternalApplyDiscountRequest request) {
        return ResponseEntity.ok(discountService.validateDiscount(request.getUserId(), request.toApplyRequest()));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApplyDiscountResponse> apply(@Valid @RequestBody InternalApplyDiscountRequest request) {
        return ResponseEntity
                .ok(discountService.validateAndApplyDiscount(request.getUserId(), request.toApplyRequest()));
    }

    public static class InternalApplyDiscountRequest {
        @NotBlank
        private String userId;

        @NotBlank
        private String code;

        @NotBlank
        private String orderId;

        @NotNull
        @Valid
        private Order order;

        public static class Order {
            @NotNull
            private BigDecimal subtotal;

            @NotNull
            private BigDecimal shippingFee;

            @NotNull
            private BigDecimal total;

            public BigDecimal getSubtotal() {
                return subtotal;
            }

            public void setSubtotal(BigDecimal subtotal) {
                this.subtotal = subtotal;
            }

            public BigDecimal getShippingFee() {
                return shippingFee;
            }

            public void setShippingFee(BigDecimal shippingFee) {
                this.shippingFee = shippingFee;
            }

            public BigDecimal getTotal() {
                return total;
            }

            public void setTotal(BigDecimal total) {
                this.total = total;
            }
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public Order getOrder() {
            return order;
        }

        public void setOrder(Order order) {
            this.order = order;
        }

        private ApplyDiscountRequest toApplyRequest() {
            ApplyDiscountRequest req = new ApplyDiscountRequest();
            req.setCode(code);
            req.setOrderId(orderId);

            ApplyDiscountRequest.Order o = new ApplyDiscountRequest.Order();
            o.setSubtotal(order.subtotal);
            o.setShippingFee(order.shippingFee);
            o.setTotal(order.total);
            // NOTE: Items intentionally omitted here. Current discount-service uses Long
            // product/category IDs,
            // while order-service uses UUID product IDs. Scope-based discounts will
            // therefore require a dedicated
            // mapping strategy. ALL-scope discounts work without items.
            o.setItems(null);
            req.setOrder(o);
            return req;
        }
    }
}
