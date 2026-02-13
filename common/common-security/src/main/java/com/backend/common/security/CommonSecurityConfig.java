package com.backend.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import reactor.core.publisher.Flux;

@Configuration
public class CommonSecurityConfig {

    @Value("${security.keycloak.roles-source:realm}")
    private String rolesSource;

    @Value("${security.keycloak.client-id:pharmacy-app}")
    private String clientId;

    @Bean
    public KeycloakJwtRoleConverter keycloakJwtRoleConverter() {
        return new KeycloakJwtRoleConverter(rolesSource, clientId);
    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(JwtAuthenticationConverter.class)
    public class ServletSecurityConfig {
        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter(
                KeycloakJwtRoleConverter keycloakJwtRoleConverter) {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(keycloakJwtRoleConverter);
            return converter;
        }
    }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(ReactiveJwtAuthenticationConverter.class)
    public class ReactiveSecurityConfig {
        @Bean
        public ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter(
                KeycloakJwtRoleConverter keycloakJwtRoleConverter) {
            ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
            converter
                    .setJwtGrantedAuthoritiesConverter(jwt -> Flux.fromIterable(keycloakJwtRoleConverter.convert(jwt)));
            return converter;
        }
    }
}
