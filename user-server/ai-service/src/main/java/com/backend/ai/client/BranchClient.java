package com.backend.ai.client;

import com.backend.ai.client.dto.BranchInternalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class BranchClient {

    private static final ParameterizedTypeReference<List<BranchInternalResponse>> ACTIVE_BRANCHES_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String baseUrl;

    public BranchClient(RestClient.Builder builder,
            @Value("${services.branch.url:http://localhost:7030}") String baseUrl) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl;
    }

    public List<BranchInternalResponse> listActiveBranches() {
        List<BranchInternalResponse> response = restClient.get()
                .uri(baseUrl + "/internal/branches/active")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ACTIVE_BRANCHES_TYPE);
        return response == null ? List.of() : response;
    }
}
