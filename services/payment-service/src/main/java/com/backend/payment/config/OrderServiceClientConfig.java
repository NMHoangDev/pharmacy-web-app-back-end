package com.backend.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OrderServiceClientConfig {

    @Bean
    public RestClient orderServiceRestClient(OrderServiceProperties properties, RestClient.Builder builder) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:7019";
        }
        return builder.baseUrl(baseUrl).build();
    }
}
