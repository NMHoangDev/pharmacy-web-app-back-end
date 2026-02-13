package com.backend.order.api.dto;

import java.util.UUID;

public record BranchSummaryDto(
                UUID id,
                String code,
                String name,
                String status,
                String addressLine,
                String ward,
                String district,
                String city,
                String province,
                String country,
                String phone,
                String timezone) {
}
