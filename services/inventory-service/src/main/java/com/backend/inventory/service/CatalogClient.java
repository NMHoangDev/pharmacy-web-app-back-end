package com.backend.inventory.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class CatalogClient {

    private static final int DEFAULT_PAGE_SIZE = 500;

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CatalogClient(RestTemplate restTemplate,
            @Value("${inventory.catalog-service.base-url:http://localhost:7017/api/catalog}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<CatalogProductDto> fetchAllProducts(String q, UUID categoryId, UUID branchId) {
        List<CatalogProductDto> results = new ArrayList<>();
        int page = 0;
        CatalogPage<CatalogProductDto> response;
        do {
            response = fetchProductPage(q, categoryId, branchId, page, DEFAULT_PAGE_SIZE);
            List<CatalogProductDto> content = response == null ? Collections.emptyList() : response.content();
            if (content == null || content.isEmpty()) {
                break;
            }
            results.addAll(content);
            page++;
        } while (response != null && response.totalPages() > page);

        return results;
    }

    public List<CatalogCategoryDto> listCategories() {
        String url = baseUrl + "/internal/categories";
        ResponseEntity<List<CatalogCategoryDto>> response = restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<CatalogCategoryDto>>() {
                });
        List<CatalogCategoryDto> body = response.getBody();
        return body == null ? Collections.emptyList() : body;
    }

    private CatalogPage<CatalogProductDto> fetchProductPage(String q, UUID categoryId, UUID branchId, int page,
            int size) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/internal/products")
                .queryParam("page", page)
                .queryParam("size", size);
        if (q != null && !q.isBlank()) {
            builder.queryParam("q", q.trim());
        }
        if (categoryId != null) {
            builder.queryParam("categoryId", categoryId);
        }
        if (branchId != null) {
            builder.queryParam("branchId", branchId);
        }
        String url = builder.toUriString();
        ResponseEntity<CatalogPage<CatalogProductDto>> response = restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<CatalogPage<CatalogProductDto>>() {
                });
        return response.getBody();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatalogProductDto(UUID id, String sku, String name, String slug, UUID categoryId,
            BigDecimal costPrice, BigDecimal baseSalePrice, BigDecimal effectivePrice, String globalStatus,
            String branchStatus, String effectiveStatus, boolean prescriptionRequired, String imageUrl,
            String attributes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatalogCategoryDto(UUID id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatalogPage<T>(List<T> content, int number, int size, int totalPages, long totalElements) {
    }
}
