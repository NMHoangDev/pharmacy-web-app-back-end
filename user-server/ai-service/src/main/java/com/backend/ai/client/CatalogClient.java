package com.backend.ai.client;

import com.backend.ai.client.dto.CatalogPageResponse;
import com.backend.ai.client.dto.CatalogProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CatalogClient {

    private final RestClient restClient;
    private final String baseUrl;

    public CatalogClient(RestClient.Builder builder,
            @Value("${services.catalog.url:http://localhost:7017}") String baseUrl) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl;
    }

    public CatalogProductDto getPublicProduct(UUID productId, UUID branchId) {
        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/api/catalog/public/products/{id}")
                .queryParamIfPresent("branchId", Optional.ofNullable(branchId))
                .buildAndExpand(productId)
                .encode(StandardCharsets.UTF_8)
                .toUri();
        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(CatalogProductDto.class);
    }

    public List<CatalogProductDto> searchPublicProducts(String query, UUID branchId, int size) {
        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/api/catalog/public/products")
                .queryParam("q", query)
                .queryParam("page", 0)
                .queryParam("size", size)
                .queryParamIfPresent("branchId", Optional.ofNullable(branchId))
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        CatalogPageResponse response = restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(CatalogPageResponse.class);
        return response == null || response.content() == null ? List.of() : response.content();
    }

    public List<CatalogProductDto> listPublicProducts(UUID branchId, int size) {
        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/api/catalog/public/products")
                .queryParam("page", 0)
                .queryParam("size", size)
                .queryParamIfPresent("branchId", Optional.ofNullable(branchId))
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        CatalogPageResponse response = restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(CatalogPageResponse.class);
        return response == null || response.content() == null ? List.of() : response.content();
    }
}
