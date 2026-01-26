package com.backend.consultation.config;

import com.backend.consultation.client.PharmacistClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PharmacistClientProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient pharmacistServiceRestClient(PharmacistClientProperties properties, RestClient.Builder builder) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }
}
