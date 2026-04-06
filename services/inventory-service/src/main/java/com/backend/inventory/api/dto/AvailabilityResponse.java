package com.backend.inventory.api.dto;

import java.util.List;

public record AvailabilityResponse(List<AvailabilityItem> items) {
}
