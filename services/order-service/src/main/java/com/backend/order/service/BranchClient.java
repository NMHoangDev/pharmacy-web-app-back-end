package com.backend.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class BranchClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public BranchClient(RestTemplate restTemplate,
            @Value("${order.branch-service.base-url:http://localhost:7030}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<BranchSummaryDto> listActive() {
        ResponseEntity<BranchSummaryDto[]> response = restTemplate.getForEntity(
                baseUrl + "/api/branches", BranchSummaryDto[].class);
        BranchSummaryDto[] body = response.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    public BranchSummaryDto getBranch(UUID branchId) {
        ResponseEntity<BranchSummaryDto> response = restTemplate.getForEntity(
                baseUrl + "/api/branches/" + branchId, BranchSummaryDto.class);
        return response.getBody();
    }

    public record BranchSummaryDto(
            UUID id,
            String code,
            String name,
            String status,
            String addressLine,
            String ward,
            String district,
            String city,
            String province,
            String country,
            String phone,
            String timezone) {
    }
}
