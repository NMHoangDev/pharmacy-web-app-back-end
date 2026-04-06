package com.backend.order.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignBranchRequest(@NotNull UUID branchId) {
}
