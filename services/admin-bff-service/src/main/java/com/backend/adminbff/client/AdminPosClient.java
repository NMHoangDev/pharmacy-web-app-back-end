package com.backend.adminbff.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
public class AdminPosClient {

    private final RestTemplate restTemplate;
    private final String pharmacistServiceBaseUrl;

    public AdminPosClient(
            RestTemplate restTemplate,
            @Value("${services.pharmacist.url:http://localhost:7025}") String pharmacistServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.pharmacistServiceBaseUrl = pharmacistServiceBaseUrl;
    }

    public List<Map<String, Object>> listAllPosOrders(UUID branchId) {
        final int size = 100;
        final int maxPages = 20;
        int page = 0;
        int totalPages = 1;
        List<Map<String, Object>> items = new ArrayList<>();

        while (page < totalPages && page < maxPages) {
            try {
                StringBuilder url = new StringBuilder(pharmacistServiceBaseUrl)
                        .append("/api/pharmacists/pos/orders?page=")
                        .append(page)
                        .append("&size=")
                        .append(size);
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
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = body != null && body.get("content") instanceof List<?> rawList
                        ? (List<Map<String, Object>>) rawList
                        : List.of();

                items.addAll(content);

                Object totalPagesObj = body != null ? body.get("totalPages") : null;
                if (totalPagesObj instanceof Number n) {
                    totalPages = Math.max(1, n.intValue());
                } else {
                    totalPages = content.size() < size ? page + 1 : page + 2;
                }
                page += 1;
            } catch (RestClientException ex) {
                throw toUpstreamError(ex);
            }
        }

        return items;
    }

    private ResponseStatusException toUpstreamError(Exception ex) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upstream pharmacist-service error", ex);
    }
}
