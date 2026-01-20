package com.backend.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        // Dev-friendly: allow admin routes through gateway without JWT.
                        // Admin BFF can still enforce its own security if needed.
                        .pathMatchers("/api/admin/**").permitAll()
                        // Allow public catalog endpoints to be called without JWT
                        .pathMatchers("/api/catalog/public/**").permitAll()
                        // Dev-friendly: allow some internal admin endpoints through gateway without JWT
                        .pathMatchers("/api/catalog/internal/**").permitAll()
                        .pathMatchers("/api/reviews/internal/**").permitAll()
                        .pathMatchers("/api/inventory/internal/**").permitAll()
                        .pathMatchers("/api/media/**").permitAll()
                        // Dev-friendly: allow user-service routes through gateway for local testing.
                        .pathMatchers("/api/users/**").permitAll()
                        .pathMatchers("/api/auth/**", "/actuator/health", "/actuator/info", "/fallback").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
