package com.backend.cart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OrderServiceClientConfig {

    @Bean
    public RestClient orderServiceRestClient(OrderServiceProperties properties, RestClient.Builder builder) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }
}
