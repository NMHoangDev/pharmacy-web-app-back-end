package com.backend.cart.api.dto;

import java.util.List;
import java.util.UUID;

public record CartResponse(UUID branchId, List<CartResponseItem> items) {
}
