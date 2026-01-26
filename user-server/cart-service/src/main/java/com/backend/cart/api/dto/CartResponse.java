package com.backend.cart.api.dto;

import java.util.List;

public record CartResponse(List<CartResponseItem> items) {
}
