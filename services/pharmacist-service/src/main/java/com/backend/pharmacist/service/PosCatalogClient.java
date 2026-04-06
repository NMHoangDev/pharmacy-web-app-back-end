package com.backend.pharmacist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PosCatalogClient {

    private static final Logger LOG = LoggerFactory.getLogger(PosCatalogClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public PosCatalogClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${pos.catalog-service.base-url:http://localhost:7017}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public ProductPage searchProducts(String query, UUID branchId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/catalog/internal/products")
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize());
        if (StringUtils.hasText(query)) {
            builder.queryParam("q", query.trim());
        }
        if (branchId != null) {
            builder.queryParam("branchId", branchId);
        }

        String uri = builder.toUriString();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            return parsePage(response.getBody(), pageable.getPageNumber(), pageable.getPageSize());
        } catch (RestClientException ex) {
            LOG.warn("Catalog service request failed at {}: {}", uri, ex.getMessage());
            return new ProductPage(List.of(), 0, pageable.getPageNumber(), pageable.getPageSize(), 0);
        }
    }

    private ProductPage parsePage(String body, int page, int size) {
        if (!StringUtils.hasText(body)) {
            return new ProductPage(List.of(), 0, page, size, 0);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            List<ProductItem> content = new ArrayList<>();
            JsonNode items = root.path("content");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    UUID id = parseUuid(item.path("id").asText(null));
                    if (id == null) {
                        continue;
                    }
                    BigDecimal effectivePrice = parseDecimal(item.path("effectivePrice"));
                    String attributes = item.path("attributes").isMissingNode() ? null
                            : item.path("attributes").asText(null);
                    content.add(new ProductItem(
                            id,
                            item.path("sku").asText(""),
                            item.path("name").asText(""),
                            item.path("imageUrl").asText(""),
                            effectivePrice,
                            attributes));
                }
            }
            long total = root.path("totalElements").asLong(content.size());
            int totalPages = root.path("totalPages").asInt(0);
            int currentPage = root.path("number").asInt(page);
            int currentSize = root.path("size").asInt(size);
            return new ProductPage(content, total, currentPage, currentSize, totalPages);
        } catch (IOException ex) {
            return new ProductPage(List.of(), 0, page, size, 0);
        }
    }

    private BigDecimal parseDecimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BigDecimal.ZERO;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        try {
            return new BigDecimal(node.asText("0"));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private UUID parseUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public record ProductPage(List<ProductItem> content, long totalElements, int page, int size, int totalPages) {
    }

    public record ProductItem(UUID id, String sku, String name, String imageUrl, BigDecimal unitPrice,
            String attributes) {
    }
}
