package com.backend.adminbff.client;

import com.backend.adminbff.dto.AdminUserProfile;
import com.backend.adminbff.dto.UpsertAdminUserRequest;
import java.util.Arrays;
import java.util.List;
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
public class AdminUserClient {

    private final RestTemplate restTemplate;
    private final String userServiceBaseUrl;

    public AdminUserClient(RestTemplate restTemplate,
            @Value("${services.user.url:http://localhost:7016}") String userServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.userServiceBaseUrl = userServiceBaseUrl;
    }

    public List<AdminUserProfile> listUsers() {
        try {
            ResponseEntity<List<AdminUserProfile>> response = restTemplate.exchange(
                    userServiceBaseUrl + "/api/users",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<AdminUserProfile>>() {
                    });
            List<AdminUserProfile> body = response.getBody();
            return body != null ? body : List.of();
        } catch (RestClientException ex) {
            throw toUpstreamError(ex);
        }
    }

    public AdminUserProfile getUser(UUID id) {
        try {
            return restTemplate.getForObject(userServiceBaseUrl + "/api/users/" + id, AdminUserProfile.class);
        } catch (RestClientException ex) {
            throw toUpstreamError(ex);
        }
    }

    public AdminUserProfile createUser(UpsertAdminUserRequest request) {
        try {
            return restTemplate.postForObject(userServiceBaseUrl + "/api/users", request, AdminUserProfile.class);
        } catch (RestClientException ex) {
            throw toUpstreamError(ex);
        }
    }

    public AdminUserProfile updateUser(UUID id, UpsertAdminUserRequest request) {
        try {
            ResponseEntity<AdminUserProfile> response = restTemplate.exchange(
                    userServiceBaseUrl + "/api/users/" + id,
                    HttpMethod.PUT,
                    new HttpEntity<>(request),
                    AdminUserProfile.class);
            return response.getBody();
        } catch (RestClientException ex) {
            throw toUpstreamError(ex);
        }
    }

    public void deleteUser(UUID id) {
        try {
            restTemplate.delete(userServiceBaseUrl + "/api/users/" + id);
        } catch (RestClientException ex) {
            throw toUpstreamError(ex);
        }
    }

    private ResponseStatusException toUpstreamError(Exception ex) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upstream user-service error", ex);
    }
}
