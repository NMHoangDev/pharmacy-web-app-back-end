package com.backend.adminbff.client;

import com.backend.adminbff.dto.AdminUserIdentitySummary;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminIdentityClient {

    private final RestTemplate restTemplate;
    private final String identityServiceBaseUrl;

    public AdminIdentityClient(
            RestTemplate restTemplate,
            @Value("${services.identity.url:http://localhost:7070}") String identityServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.identityServiceBaseUrl = identityServiceBaseUrl;
    }

    public List<AdminUserIdentitySummary> listUserIdentitySummaries() {
        try {
            ResponseEntity<List<AdminUserIdentitySummary>> response = restTemplate.exchange(
                    identityServiceBaseUrl + "/api/auth/admin/users/identity",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<AdminUserIdentitySummary>>() {
                    });
            List<AdminUserIdentitySummary> body = response.getBody();
            return body != null ? body : List.of();
        } catch (RestClientException ex) {
            throw toUpstreamError(ex);
        }
    }

    private ResponseStatusException toUpstreamError(Exception ex) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upstream identity-service error", ex);
    }
}
