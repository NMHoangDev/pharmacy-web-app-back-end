package com.backend.adminbff.client;

import com.backend.adminbff.dto.AdminOrderResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class AdminOrderClient {

    private final RestTemplate restTemplate;
    private final String orderServiceBaseUrl;

    public AdminOrderClient(RestTemplate restTemplate,
            @Value("${services.order.url:http://localhost:7019}") String orderServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceBaseUrl = orderServiceBaseUrl;
    }

    public List<AdminOrderResponse> listOrders(String status, UUID userId) {
        try {
            StringBuilder url = new StringBuilder(orderServiceBaseUrl + "/api/admin/orders");
            boolean hasQuery = false;
            if (status != null && !status.isBlank()) {
                url.append("?status=").append(status);
                hasQuery = true;
            }
            if (userId != null) {
                url.append(hasQuery ? "&" : "?").append("userId=").append(userId);
            }

            ResponseEntity<List<AdminOrderResponse>> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<AdminOrderResponse>>() {
                    });
            List<AdminOrderResponse> body = response.getBody();
            return body != null ? body : List.of();
        } catch (RestClientException ex) {
            throw toUpstreamError(ex);
        }
    }

    public AdminOrderResponse getOrder(UUID id) {
        try {
            return restTemplate.getForObject(orderServiceBaseUrl + "/api/admin/orders/" + id,
                    AdminOrderResponse.class);
        } catch (RestClientException ex) {
            throw toUpstreamError(ex);
        }
    }

    public AdminOrderResponse updateStatus(UUID id, String status) {
        try {
            ResponseEntity<AdminOrderResponse> response = restTemplate.exchange(
                    orderServiceBaseUrl + "/api/admin/orders/" + id + "/status",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("status", status)),
                    AdminOrderResponse.class);
            return response.getBody();
        } catch (RestClientException ex) {
            throw toUpstreamError(ex);
        }
    }

    public AdminOrderResponse cancelOrder(UUID id) {
        try {
            return restTemplate.postForObject(orderServiceBaseUrl + "/api/admin/orders/" + id + "/cancel",
                    null,
                    AdminOrderResponse.class);
        } catch (RestClientException ex) {
            throw toUpstreamError(ex);
        }
    }

    private ResponseStatusException toUpstreamError(Exception ex) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upstream order-service error", ex);
    }
}