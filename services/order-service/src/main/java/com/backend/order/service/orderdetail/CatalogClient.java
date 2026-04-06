package com.backend.order.service.orderdetail;

import com.backend.order.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class CatalogClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CatalogClient(
            RestTemplate restTemplate,
            @Value("${order.catalog-service.base-url:http://localhost:7017}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public ProductSnapshot getProduct(UUID productId) {
        try {
            ResponseEntity<DrugAdminDto> response = restTemplate.getForEntity(
                    baseUrl + "/api/catalog/internal/products/" + productId,
                    DrugAdminDto.class);
            DrugAdminDto body = response.getBody();
            if (body == null) {
                return null;
            }
            String category = body.categoryId() == null ? null : body.categoryId().toString();
            return new ProductSnapshot(
                    body.name(),
                    body.imageUrl(),
                    body.sku(),
                    null,
                    category,
                    null,
                    body.description());
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch product from catalog-service", ex);
        }
    }

    private record DrugAdminDto(
            UUID id,
            String sku,
            String name,
            String slug,
            UUID categoryId,
            BigDecimal costPrice,
            BigDecimal baseSalePrice,
            BigDecimal priceOverride,
            BigDecimal effectivePrice,
            String globalStatus,
            String branchStatus,
            String effectiveStatus,
            boolean prescriptionRequired,
            String description,
            String imageUrl,
            String attributes,
            UUID branchId,
            String note) {
    }
}
