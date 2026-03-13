package com.backend.gateway.config;

import com.backend.common.security.CommonSecurityConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@Import(CommonSecurityConfig.class)
public class SecurityConfig {

        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
        private String issuerUri;

        @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
        private String jwkSetUri;

        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                        ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter) {
                http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .cors(org.springframework.security.config.Customizer.withDefaults())
                                .authorizeExchange(exchanges -> exchanges
                                                // Public
                                                .pathMatchers("/actuator/health/**", "/actuator/info/**", "/fallback")
                                                .permitAll()
                                                .pathMatchers("/api/auth/**").permitAll()
                                                .pathMatchers(HttpMethod.GET, "/api/branches/**", "/api/branches")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.GET, "/api/catalog/products/**",
                                                                "/api/catalog/public/**", "/api/media/public/**",
                                                                "/api/media/**")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.GET, "/api/content/**")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.GET, "/api/reviews/product/**",
                                                                "/api/reviews/ping")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.POST, "/api/content/posts/*/view")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.OPTIONS).permitAll()

                                                // ADMIN
                                                .pathMatchers("/api/reviews/internal/**")
                                                .hasRole("ADMIN")

                                                // USER
                                                .pathMatchers("/api/cart/**", "/api/orders/**", "/api/order/**",
                                                                "/api/payments/**", "/api/reviews/**")
                                                .hasAnyRole("USER", "ADMIN")
                                                .pathMatchers("/api/appointments/**", "/api/appointments")
                                                .hasAnyRole("USER", "PHARMACIST", "ADMIN")
                                                .pathMatchers("/api/users/me/**")
                                                .hasAnyRole("USER", "PHARMACIST", "STAFF", "ADMIN")
                                                .pathMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")

                                                // STAFF
                                                .pathMatchers("/api/inventory/**", "/api/cms/**")
                                                .hasAnyRole("STAFF", "ADMIN")

                                                // PHARMACIST
                                                .pathMatchers("/api/pharmacists/**")
                                                .hasAnyRole("PHARMACIST", "ADMIN", "USER")

                                                // ADMIN
                                                .pathMatchers("/api/admin/**", "/api/audit/**", "/api/settings/**",
                                                                "/api/reporting/**")
                                                .hasRole("ADMIN")

                                                // Default
                                                .anyExchange().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                reactiveJwtAuthenticationConverter)));

                return http.build();
        }

        @Bean
        public ReactiveJwtDecoder jwtDecoder() {
                String resolvedJwkSetUri = (jwkSetUri == null || jwkSetUri.isBlank())
                                ? issuerUri + "/protocol/openid-connect/certs"
                                : jwkSetUri;

                NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(resolvedJwkSetUri)
                                .build();

                OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
                OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
                        return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
                };

                jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));

                return jwtDecoder;
        }
}
