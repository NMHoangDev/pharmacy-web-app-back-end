package com.backend.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import reactor.core.publisher.Flux;

@Configuration(proxyBeanMethods = false)
public class CommonSecurityConfig {

    @Value("${security.keycloak.roles-source:realm}")
    private String rolesSource;

    @Value("${security.keycloak.client-id:pharmacy-app}")
    private String clientId;

    // 👉 Converter dùng chung cho tất cả services
    @Bean
    @ConditionalOnMissingBean
    public KeycloakJwtRoleConverter keycloakJwtRoleConverter() {
        return new KeycloakJwtRoleConverter(rolesSource, clientId);
    }

    // ================== SERVLET ==================
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(JwtAuthenticationConverter.class)
    static class ServletSecurityConfig {

        @Bean
        @ConditionalOnMissingBean
        public JwtAuthenticationConverter jwtAuthenticationConverter(
                KeycloakJwtRoleConverter roleConverter) {

            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

            // 👉 chỉ map role, KHÔNG đụng authentication flow
            converter.setJwtGrantedAuthoritiesConverter(roleConverter);

            return converter;
        }
    }

    // ================== REACTIVE ==================
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(ReactiveJwtAuthenticationConverter.class)
    static class ReactiveSecurityConfig {

        @Bean
        @ConditionalOnMissingBean
        public ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter(
                KeycloakJwtRoleConverter roleConverter) {

            ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();

            converter.setJwtGrantedAuthoritiesConverter(
                    jwt -> Flux.fromIterable(roleConverter.convert(jwt)));

            return converter;
        }
    }
}