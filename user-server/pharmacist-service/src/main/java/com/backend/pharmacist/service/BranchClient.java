package com.backend.pharmacist.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class BranchClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final long cacheTtlSeconds;

    private volatile Instant staffCacheExpiresAt = Instant.EPOCH;
    private volatile UUID cachedBranchId;
    private volatile List<UUID> cachedPharmacistIds = Collections.emptyList();

    public BranchClient(
            RestTemplate restTemplate,
            @Value("${branch.base-url}") String baseUrl,
            @Value("${branch.staff-cache-ttl-seconds:60}") long cacheTtlSeconds) {
        this.restTemplate = restTemplate;
        this.baseUrl = Objects.requireNonNull(baseUrl, "branch.base-url required");
        this.cacheTtlSeconds = Math.max(0, cacheTtlSeconds);
    }

    public List<UUID> getBranchPharmacistIds(UUID branchId) {
        if (branchId == null) {
            return null;
        }
        if (isCacheValidFor(branchId)) {
            return cachedPharmacistIds;
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
            List<UUID> ids = staff == null
                    ? Collections.emptyList()
                    : staff.stream().map(BranchStaffResponse::userId).toList();
            cache(branchId, ids);
            return ids;
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }

    public BranchSummary getBranch(UUID branchId) {
        if (branchId == null) {
            return null;
        }
        try {
            String url = buildUrl(String.format("/internal/branches/%s", branchId));
            return restTemplate.getForObject(url, BranchSummary.class);
        } catch (RestClientException ex) {
            return null;
        }
    }

    public UUID getPrimaryBranchId(UUID userId, String role) {
        if (userId == null) {
            return null;
        }
        try {
            String url = buildUrl(String.format("/internal/branches/staff/%s/primary?role=%s", userId, normalizeRole(role)));
            BranchStaffResponse response = restTemplate.getForObject(url, BranchStaffResponse.class);
            return response == null ? null : response.branchId();
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        } catch (RestClientException ex) {
            return null;
        }
    }

    public void assignPrimaryBranch(UUID userId, UUID branchId, String role) {
        if (userId == null) {
            return;
        }
        try {
            String url = buildUrl(String.format("/internal/branches/staff/%s/primary", userId));
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(new BranchPrimaryStaffRequest(branchId, normalizeRole(role), null, true)),
                    BranchStaffResponse.class);
        } catch (RestClientException ex) {
            // Do not fail pharmacist updates if branch-service sync is temporarily unavailable.
        }
    }

    private boolean isCacheValidFor(UUID branchId) {
        if (cacheTtlSeconds <= 0) {
            return false;
        }
        return branchId.equals(cachedBranchId) && Instant.now().isBefore(staffCacheExpiresAt);
    }

    private void cache(UUID branchId, List<UUID> pharmacistIds) {
        this.cachedBranchId = branchId;
        this.cachedPharmacistIds = pharmacistIds == null ? Collections.emptyList() : pharmacistIds;
        this.staffCacheExpiresAt = Instant.now().plus(Duration.ofSeconds(cacheTtlSeconds));
    }

    private String normalizeRole(String role) {
        return role == null || role.isBlank() ? "PHARMACIST" : role.trim().toUpperCase();
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

    private record BranchStaffResponse(UUID branchId, UUID userId, String role, String skillsJson, boolean active) {
    }

    public record BranchSummary(UUID id, String code, String name, String status, String timezone) {
    }

    private record BranchPrimaryStaffRequest(UUID branchId, String role, String skillsJson, Boolean active) {
    }
}
