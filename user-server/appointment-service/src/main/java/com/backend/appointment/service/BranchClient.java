package com.backend.appointment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class BranchClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public BranchClient(RestTemplate restTemplate,
            @Value("${branch.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = Objects.requireNonNull(baseUrl, "branch.base-url required");
    }

    public boolean isBranchActive(UUID branchId) {
        if (branchId == null) {
            return true;
        }
        try {
            String url = buildUrl(String.format("/internal/branches/%s", branchId));
            ResponseEntity<BranchInternalResponse> response = restTemplate.exchange(
                    url,
                    Objects.requireNonNull(HttpMethod.GET),
                    HttpEntity.EMPTY,
                    BranchInternalResponse.class);
            BranchInternalResponse body = response.getBody();
            return body != null && body.active();
        } catch (RestClientException ex) {
            return false;
        }
    }

    public List<UUID> getBranchPharmacistIds(UUID branchId) {
        if (branchId == null) {
            return null;
        }
        try {
            String url = buildUrl(String.format("/internal/branches/%s/staff?role=PHARMACIST", branchId));
            ResponseEntity<List<BranchStaffResponse>> response = restTemplate.exchange(
                    url,
                    Objects.requireNonNull(HttpMethod.GET),
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<List<BranchStaffResponse>>() {
                    });
            List<BranchStaffResponse> staff = response.getBody();
            if (staff == null) {
                return Collections.emptyList();
            }
            return staff.stream().map(BranchStaffResponse::userId).toList();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }

    private record BranchInternalResponse(UUID id, String name, String code, boolean active) {
    }

    private record BranchStaffResponse(UUID branchId, UUID userId, String role, String skillsJson, boolean active) {
    }

    private @NonNull String buildUrl(String path) {
        if (path == null || path.isBlank()) {
            return Objects.requireNonNull(baseUrl);
        }
        if (path.startsWith("/")) {
            return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + path : baseUrl + path;
        }
        return baseUrl.endsWith("/") ? baseUrl + path : baseUrl + "/" + path;
    }
}
