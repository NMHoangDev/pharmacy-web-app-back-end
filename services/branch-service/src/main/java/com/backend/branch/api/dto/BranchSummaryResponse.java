package com.backend.branch.api.dto;

import java.util.UUID;

public record BranchSummaryResponse(
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
