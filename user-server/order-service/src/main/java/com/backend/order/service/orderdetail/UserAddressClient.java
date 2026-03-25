package com.backend.order.service.orderdetail;

import com.backend.order.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class UserAddressClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserAddressClient(
            RestTemplate restTemplate,
            @Value("${order.user-service.base-url:http://localhost:7016}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public AddressSnapshot getAddress(UUID userId, UUID addressId) {
        try {
            ResponseEntity<AddressResponse[]> response = restTemplate.getForEntity(
                    baseUrl + "/api/users/" + userId + "/addresses",
                    AddressResponse[].class);
            AddressResponse[] body = response.getBody();
            List<AddressResponse> addresses = body == null ? List.of() : Arrays.asList(body);
            return addresses.stream()
                    .filter(a -> addressId.equals(a.id()))
                    .findFirst()
                    .map(this::toSnapshot)
                    .orElse(null);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch address from user-service", ex);
        }
    }

    private AddressSnapshot toSnapshot(AddressResponse address) {
        String fullAddress = buildFullAddress(address.line1(), address.line2(), address.city(), address.state(),
                address.country());
        return new AddressSnapshot(
                address.id(),
                null,
                null,
                address.state(),
                address.city(),
                null,
                null,
                address.line1(),
                address.line2(),
                fullAddress,
                address.label(),
                address.isDefault());
    }

    private String buildFullAddress(String line1, String line2, String city, String state, String country) {
        return String.join(", ",
                List.of(line1, line2, city, state, country).stream()
                        .filter(v -> v != null && !v.isBlank())
                        .toList());
    }

    private record AddressResponse(
            UUID id,
            String label,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country,
            Boolean isDefault) {
    }
}
