package com.backend.gateway.config;

import com.backend.common.security.CommonSecurityConfig;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@Import(CommonSecurityConfig.class)
public class SecurityConfig {

        private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
        private String issuerUri;

        @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
        private String jwkSetUri;

        @Value("${CORS_ALLOWED_ORIGIN_PATTERNS:http://localhost:3000,http://192.168.56.1:3000}")
        private String corsAllowedOriginPatterns;

        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                        ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter) {
                http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeExchange(exchanges -> exchanges
                                                // Public
                                                .pathMatchers("/actuator/health/**", "/actuator/info/**", "/fallback")
                                                .permitAll()
                                                .pathMatchers("/ws", "/ws/**")
                                                .permitAll()
                                                .pathMatchers("/api/auth/**").permitAll()
                                                .pathMatchers("/api/chat", "/api/chat/**").permitAll()
                                                .pathMatchers(HttpMethod.GET, "/api/branches/**", "/api/branches")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.GET, "/api/catalog/products/**",
                                                                "/api/catalog/public/**", "/api/media/public/**",
                                                                "/api/media/**")
                                                .permitAll()
                                                .pathMatchers("/api/content/admin/**")
                                                .hasRole("ADMIN")
                                                .pathMatchers(HttpMethod.GET,
                                                                "/api/content/public/**",
                                                                "/api/content/posts",
                                                                "/api/content/posts/**",
                                                                "/api/content/questions",
                                                                "/api/content/questions/**",
                                                                "/api/content/tags",
                                                                "/api/content/tags/**")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.GET, "/api/reviews/product/**",
                                                                "/api/reviews/ping")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.POST, "/api/content/posts/*/view")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.OPTIONS).permitAll()

                                                // DISCOUNT (non-/api prefixed)
                                                .pathMatchers(HttpMethod.GET, "/user/discounts/campaigns").permitAll()
                                                .pathMatchers("/admin/**").hasRole("ADMIN")
                                                .pathMatchers("/user/**").authenticated()

                                                // ADMIN
                                                .pathMatchers("/api/reviews/internal/**")
                                                .hasRole("ADMIN")

                                                // USER
                                                .pathMatchers(HttpMethod.GET, "/api/pharmacists/**")
                                                .permitAll()
                                                .pathMatchers("/api/cart/**", "/api/orders/**", "/api/order/**",
                                                                "/api/payments/**", "/api/reviews/**")
                                                .authenticated()
                                                .pathMatchers("/api/appointments/**", "/api/appointments")
                                                .hasAnyRole("USER", "PHARMACIST", "ADMIN")
                                                .pathMatchers("/api/users/me/**")
                                                .hasAnyRole("USER", "PHARMACIST", "STAFF", "ADMIN")
                                                .pathMatchers("/api/users/**").authenticated()

                                                // STAFF
                                                .pathMatchers("/api/inventory/**", "/api/cms/**")
                                                .hasAnyRole("STAFF", "ADMIN")

                                                // PHARMACIST
                                                .pathMatchers("/api/pharmacists/**")
                                                .authenticated()

                                                // ADMIN
                                                .pathMatchers("/api/admin/**", "/api/audit/**", "/api/settings/**",
                                                                "/api/reporting/**")
                                                .hasRole("ADMIN")

                                                // Default
                                                .anyExchange().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .bearerTokenConverter(publicAwareBearerTokenConverter())
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                reactiveJwtAuthenticationConverter)));

                return http.build();
        }

        @Bean
        public ServerAuthenticationConverter publicAwareBearerTokenConverter() {
                ServerBearerTokenAuthenticationConverter defaultConverter = new ServerBearerTokenAuthenticationConverter();

                return exchange -> {
                        String path = exchange.getRequest().getPath().pathWithinApplication().value();
                        HttpMethod method = exchange.getRequest().getMethod();

                        if (isPublicEndpoint(method, path)) {
                                // Permit-all endpoints should not fail because of stale/invalid bearer token.
                                return Mono.empty();
                        }

                        return defaultConverter.convert(exchange);
                };
        }

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public WebFilter stripAuthorizationOnPublicEndpointsFilter() {
                return (exchange, chain) -> {
                        String path = exchange.getRequest().getPath().pathWithinApplication().value();
                        HttpMethod method = exchange.getRequest().getMethod();

                        if (!isPublicEndpoint(method, path)) {
                                return chain.filter(exchange);
                        }

                        ServerWebExchange mutated = exchange.mutate()
                                        .request(request -> request.headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION)))
                                        .build();

                        return chain.filter(mutated);
                };
        }

        private boolean isPublicEndpoint(HttpMethod method, String path) {
                if (path == null) {
                        return false;
                }

                if (HttpMethod.OPTIONS.equals(method)) {
                        return true;
                }

                if (PATH_MATCHER.match("/actuator/health/**", path)
                                || PATH_MATCHER.match("/actuator/info/**", path)
                                || PATH_MATCHER.match("/fallback", path)
                                || PATH_MATCHER.match("/ws", path)
                                || PATH_MATCHER.match("/ws/**", path)
                                || PATH_MATCHER.match("/api/auth/**", path)
                                || PATH_MATCHER.match("/api/chat", path)
                                || PATH_MATCHER.match("/api/chat/health", path)) {
                        return true;
                }

                if (HttpMethod.GET.equals(method)) {
                        return PATH_MATCHER.match("/api/branches", path)
                                        || PATH_MATCHER.match("/api/branches/**", path)
                                        || PATH_MATCHER.match("/api/catalog/products/**", path)
                                        || PATH_MATCHER.match("/api/catalog/public/**", path)
                                        || PATH_MATCHER.match("/api/media/public/**", path)
                                        || PATH_MATCHER.match("/api/media/**", path)
                                        || PATH_MATCHER.match("/api/content/public/**", path)
                                        || PATH_MATCHER.match("/api/content/posts", path)
                                        || PATH_MATCHER.match("/api/content/posts/**", path)
                                        || PATH_MATCHER.match("/api/content/questions", path)
                                        || PATH_MATCHER.match("/api/content/questions/**", path)
                                        || PATH_MATCHER.match("/api/content/tags", path)
                                        || PATH_MATCHER.match("/api/content/tags/**", path)
                                        || PATH_MATCHER.match("/api/reviews/product/**", path)
                                        || PATH_MATCHER.match("/api/reviews/ping", path)
                                        || PATH_MATCHER.match("/api/pharmacists/**", path)
                                        || PATH_MATCHER.match("/user/discounts/campaigns", path);
                }

                if (HttpMethod.POST.equals(method) && PATH_MATCHER.match("/api/content/posts/*/view", path)) {
                        return true;
                }

                return false;
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                List<String> originPatterns = Arrays.stream(corsAllowedOriginPatterns.split(","))
                                .map(String::trim)
                                .filter(value -> !value.isBlank())
                                .collect(Collectors.toList());

                configuration.setAllowedOriginPatterns(originPatterns);
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
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
