package com.backend.adminbff.repository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Repository
public class AdminMedicineStatsRepository {

    private final RestTemplate restTemplate;
    private final String catalogServiceBaseUrl;
    private final String inventoryServiceBaseUrl;

    public AdminMedicineStatsRepository(
            RestTemplate restTemplate,
            @Value("${services.catalog.url:http://localhost:7017}") String catalogServiceBaseUrl,
            @Value("${services.inventory.url:http://localhost:7018}") String inventoryServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.catalogServiceBaseUrl = catalogServiceBaseUrl;
        this.inventoryServiceBaseUrl = inventoryServiceBaseUrl;
    }

    public List<Map<String, Object>> findAllCatalogProducts() {
        int page = 0;
        int totalPages = 1;
        List<Map<String, Object>> all = new ArrayList<>();

        while (page < totalPages) {
            String url = String.format(
                    "%s/api/catalog/internal/products?page=%d&size=200&sort=name,asc",
                    catalogServiceBaseUrl,
                    page);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> body = response.getBody();
            if (body == null) {
                break;
            }

            Object content = body.get("content");
            if (content instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map<?, ?> mapItem) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> typed = (Map<String, Object>) mapItem;
                        all.add(typed);
                    }
                }
            }

            totalPages = parseInt(body.get("totalPages"), 1);
            page++;
        }

        return all;
    }

    public Map<UUID, Integer> findAvailableStockByProductIds(List<UUID> productIds) {
        Map<UUID, Integer> stockByProductId = new java.util.HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return stockByProductId;
        }

        int chunkSize = 100;
        for (int i = 0; i < productIds.size(); i += chunkSize) {
            List<UUID> chunk = productIds.subList(i, Math.min(i + chunkSize, productIds.size()));
            String joined = chunk.stream().map(UUID::toString).reduce((a, b) -> a + "," + b).orElse("");
            String encoded = URLEncoder.encode(joined, StandardCharsets.UTF_8);

            String url = String.format(
                    "%s/api/inventory/internal/inventory/availability?productIds=%s",
                    inventoryServiceBaseUrl,
                    encoded);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> body = response.getBody();
            if (body == null) {
                continue;
            }

            Object itemsObj = body.get("items");
            if (!(itemsObj instanceof List<?> items)) {
                continue;
            }

            for (Object item : items) {
                if (!(item instanceof Map<?, ?> raw)) {
                    continue;
                }
                Object productIdRaw = raw.get("productId");
                Object availableRaw = raw.get("available");
                if (productIdRaw == null) {
                    continue;
                }
                try {
                    UUID productId = UUID.fromString(String.valueOf(productIdRaw));
                    int available = parseInt(availableRaw, 0);
                    stockByProductId.put(productId, available);
                } catch (IllegalArgumentException ignore) {
                    // Ignore malformed upstream rows.
                }
            }
        }

        return stockByProductId;
    }

    private int parseInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
