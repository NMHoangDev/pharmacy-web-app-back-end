package com.pharmacy.discount.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public class ApplyDiscountRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String orderId;

    @NotNull
    @Valid
    private Order order;

    public static class Order {
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal subtotal;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal shippingFee;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal total;

        @Valid
        private List<Item> items;

        public static class Item {
            @NotNull
            private Long productId;
            private Long categoryId;
            @NotNull
            @Min(1)
            private Integer quantity;
            @NotNull
            @DecimalMin(value = "0.0", inclusive = true)
            private BigDecimal unitPrice;

            public Long getProductId() {
                return productId;
            }

            public void setProductId(Long productId) {
                this.productId = productId;
            }

            public Long getCategoryId() {
                return categoryId;
            }

            public void setCategoryId(Long categoryId) {
                this.categoryId = categoryId;
            }

            public Integer getQuantity() {
                return quantity;
            }

            public void setQuantity(Integer quantity) {
                this.quantity = quantity;
            }

            public BigDecimal getUnitPrice() {
                return unitPrice;
            }

            public void setUnitPrice(BigDecimal unitPrice) {
                this.unitPrice = unitPrice;
            }
        }

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

        public List<Item> getItems() {
            return items;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }
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
}
