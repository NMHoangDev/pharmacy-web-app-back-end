package com.backend.consultation.client;

import com.backend.consultation.client.dto.PharmacistShiftDto;
import com.backend.consultation.client.dto.PharmacistDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class PharmacistClient {

    private final RestClient restClient;
    private final String baseUrl;

    public PharmacistClient(RestClient restClient, PharmacistClientProperties properties) {
        this.restClient = restClient;
        this.baseUrl = properties.getBaseUrl();
    }

    public PharmacistDto getPharmacist(UUID id, String authorization) {
        try {
            var uri = URI.create(baseUrl + "/api/pharmacists/" + id.toString());
            RequestEntity<Void> req = RequestEntity.get(uri)
                    .header(HttpHeaders.AUTHORIZATION, normalizeAuth(authorization)).build();
            ResponseEntity<PharmacistDto> resp = restClient.exchange(req, PharmacistDto.class);
            return resp.getBody();
        } catch (RestClientResponseException ex) {
            throw new RestClientException("pharmacist-service returned " + ex.getRawStatusCode(), ex);
        }
    }

    public List<PharmacistShiftDto> getShifts(UUID pharmacistId, LocalDateTime from, LocalDateTime to,
            String authorization) {
        try {
            var uri = URI.create(
                    baseUrl + "/api/pharmacists/" + pharmacistId.toString() + "/shifts?from=" + from + "&to=" + to);
            RequestEntity<Void> req = RequestEntity.get(uri)
                    .header(HttpHeaders.AUTHORIZATION, normalizeAuth(authorization)).build();
            ResponseEntity<PharmacistShiftDto[]> resp = restClient.exchange(req, PharmacistShiftDto[].class);
            PharmacistShiftDto[] arr = resp.getBody();
            return arr == null ? List.of() : List.of(arr);
        } catch (RestClientResponseException ex) {
            throw new RestClientException("pharmacist-service returned " + ex.getRawStatusCode(), ex);
        }
    }

    private String normalizeAuth(String authorization) {
        if (authorization == null)
            return null;
        if (authorization.toLowerCase().startsWith("bearer "))
            return authorization;
        return "Bearer " + authorization;
    }
}
