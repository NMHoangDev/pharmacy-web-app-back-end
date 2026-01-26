package com.backend.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class CartItemId implements Serializable {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cart_user_id", length = 36)
    private UUID cartUserId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", length = 36)
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
