package com.backend.cart.api.dto;

import java.util.UUID;

public record CartResponseItem(UUID productId, int quantity) {
}
