package com.backend.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CartItemId implements Serializable {

    @Column(name = "cart_user_id", columnDefinition = "char(36)")
    private UUID cartUserId;

    @Column(name = "product_id", columnDefinition = "char(36)")
    private UUID productId;

    public CartItemId() {
    }

    public CartItemId(UUID cartUserId, UUID productId) {
        this.cartUserId = cartUserId;
        this.productId = productId;
    }

    public UUID getCartUserId() {
        return cartUserId;
    }

    public void setCartUserId(UUID cartUserId) {
        this.cartUserId = cartUserId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CartItemId that = (CartItemId) o;
        return Objects.equals(cartUserId, that.cartUserId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartUserId, productId);
    }
}
