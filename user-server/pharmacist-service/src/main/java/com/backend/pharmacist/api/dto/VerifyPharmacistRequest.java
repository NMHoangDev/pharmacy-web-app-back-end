package com.backend.pharmacist.api.dto;

import jakarta.validation.constraints.NotNull;

public record VerifyPharmacistRequest(@NotNull Boolean verified) {
}
