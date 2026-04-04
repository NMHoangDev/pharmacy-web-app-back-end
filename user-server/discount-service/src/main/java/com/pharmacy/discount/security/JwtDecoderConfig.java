package com.pharmacy.discount.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${security.keycloak.audience:}")
    private String audience;

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Bean
    public JwtDecoder jwtDecoder() {
        if (!securityEnabled) {
            // Local/dev escape hatch: allow the app to start without requiring Keycloak.
            // The security filter chain should be configured to permit all when disabled.
            return token -> {
                throw new IllegalStateException("JWT decoding is disabled (app.security.enabled=false)");
            };
        }

        NimbusJwtDecoder decoder;
        OAuth2TokenValidator<Jwt> baseValidator;

        // Prefer issuer-based decoder/validator when configured (recommended for
        // Keycloak).
        if (issuerUri != null && !issuerUri.isBlank()) {
            decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);
            baseValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        } else if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            baseValidator = JwtValidators.createDefault();
        } else {
            throw new IllegalStateException("Missing JWT configuration: set issuer-uri or jwk-set-uri");
        }
        if (audience == null || audience.isBlank()) {
            decoder.setJwtValidator(baseValidator);
            return decoder;
        }

        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(audience);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(baseValidator, withAudience));
        return decoder;
    }
}
