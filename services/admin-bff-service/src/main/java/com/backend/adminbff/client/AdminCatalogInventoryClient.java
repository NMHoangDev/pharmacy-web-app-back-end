package com.backend.adminbff.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class AdminCatalogInventoryClient {

    private final RestTemplate restTemplate;
    private final String catalogServiceBaseUrl;
    private final String inventoryServiceBaseUrl;

    public AdminCatalogInventoryClient(
            RestTemplate restTemplate,
            @Value("${services.catalog.url:http://localhost:7017}") String catalogServiceBaseUrl,
            @Value("${services.inventory.url:http://localhost:7018}") String inventoryServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.catalogServiceBaseUrl = catalogServiceBaseUrl;
        this.inventoryServiceBaseUrl = inventoryServiceBaseUrl;
    }

    public List<Map<String, Object>> listCatalogProducts(UUID branchId) {
        try {
            StringBuilder url = new StringBuilder(catalogServiceBaseUrl)
                    .append("/api/catalog/internal/products?page=0&size=300");
            if (branchId != null) {
                url.append("&branchId=").append(branchId);
            }

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = body.get("content") instanceof List<?> rawList
                    ? (List<Map<String, Object>>) rawList
                    : List.of();
            return content;
        } catch (RestClientException ex) {
            throw toCatalogError(ex);
        }
    }

    public List<Map<String, Object>> getInventoryAvailability(List<UUID> productIds, UUID branchId) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        try {
            String productIdParam = productIds.stream().map(UUID::toString).collect(Collectors.joining(","));
            StringBuilder url = new StringBuilder(inventoryServiceBaseUrl)
                    .append("/api/inventory/internal/inventory/availability?productIds=")
                    .append(productIdParam);
            if (branchId != null) {
                url.append("&branchId=").append(branchId);
            }

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = body.get("items") instanceof List<?> rawList
                    ? (List<Map<String, Object>>) rawList
                    : List.of();
            return items;
        } catch (RestClientException ex) {
            throw toInventoryError(ex);
        }
    }

    private ResponseStatusException toCatalogError(Exception ex) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upstream catalog-service error", ex);
    }

    private ResponseStatusException toInventoryError(Exception ex) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upstream inventory-service error", ex);
    }
}
