package com.backend.ai.client;

import com.backend.ai.client.dto.InventoryAvailabilityBatchRequest;
import com.backend.ai.client.dto.InventoryAvailabilityBatchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InventoryClient {

    private final RestClient restClient;
    private final String baseUrl;

    public InventoryClient(RestClient.Builder builder,
            @Value("${services.inventory.url:http://localhost:7018}") String baseUrl) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl;
    }

    public InventoryAvailabilityBatchResponse availabilityBatch(InventoryAvailabilityBatchRequest request) {
        return restClient.post()
                .uri(baseUrl + "/api/inventory/internal/inventory/availability/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(InventoryAvailabilityBatchResponse.class);
    }
}
