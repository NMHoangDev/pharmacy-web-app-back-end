package com.backend.order.api.dto;

import java.util.List;

public record CartResponse(List<CartResponseItem> items) {
}
