package com.backend.inventory.api.dto;

import java.util.List;

public record AvailabilityBatchResponse(List<AvailabilityBatchItem> items) {
}
