package com.backend.audit.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuditRecordRequest(
        @NotBlank @Size(max = 255) String actor,
        @NotBlank @Size(max = 255) String action,
        @NotBlank @Size(max = 255) String resource,
        @Size(max = 2048) String metadata) {
}
