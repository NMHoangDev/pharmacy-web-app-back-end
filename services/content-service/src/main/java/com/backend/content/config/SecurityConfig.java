package com.backend.content.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Value("${security.keycloak.client-id:pharmacy-app}")
        private String keycloakClientId;

        @Value("${CORS_ALLOWED_ORIGIN_PATTERNS:${app.cors.allowed-origin-patterns:http://localhost:3000,http://192.168.56.1:3000,http://localhost:*}}")
        private String corsAllowedOriginPatterns;

        // ==================== CORS Configuration ====================
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                List<String> originPatterns = Arrays.stream(corsAllowedOriginPatterns.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.toList());

                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(
                                originPatterns.isEmpty() ? List.of("http://localhost:3000", "http://192.168.56.1:3000")
                                                : originPatterns);
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
                config.setMaxAge(3600L);
                config.setAllowCredentials(false);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }

        // ==================== JWT Role Converter ====================
        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtRoleConverter(keycloakClientId));
                return converter;
        }

        private class KeycloakJwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
                private final String clientId;

                KeycloakJwtRoleConverter(String clientId) {
                        this.clientId = clientId;
                }

                @Override
                public Collection<GrantedAuthority> convert(Jwt jwt) {
                        Set<String> roles = new HashSet<>();

                        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                        if (realmAccess != null && realmAccess.containsKey("roles")) {
                                roles.addAll((Collection<String>) realmAccess.get("roles"));
                        }

                        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
                        if (resourceAccess != null && resourceAccess.containsKey(clientId)) {
                                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
                                if (clientAccess != null && clientAccess.containsKey("roles")) {
                                        roles.addAll((Collection<String>) clientAccess.get("roles"));
                                }
                        }

                        return roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .collect(Collectors.toList());
                }
        }

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer(RequestMatcher publicEndpointsMatcher) {
                return (WebSecurity web) -> web.ignoring().requestMatchers(publicEndpointsMatcher);
        }

        @Bean
        @Order(1)
        public SecurityFilterChain publicSecurityFilterChain(
                        HttpSecurity http,
                        CorsConfigurationSource corsConfigurationSource,
                        RequestMatcher publicEndpointsMatcher,
                        OncePerRequestFilter publicEndpointAuthorizationStripFilter) throws Exception {

                http
                        .securityMatcher(publicEndpointsMatcher)
                        .csrf(AbstractHttpConfigurer::disable)
                        .cors(cors -> cors.configurationSource(corsConfigurationSource))
                        .formLogin(AbstractHttpConfigurer::disable)
                        .httpBasic(AbstractHttpConfigurer::disable)
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .addFilterBefore(publicEndpointAuthorizationStripFilter, BearerTokenAuthenticationFilter.class)
                        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain protectedSecurityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter,
                        CorsConfigurationSource corsConfigurationSource,
                        RequestMatcher publicEndpointsMatcher,
                        BearerTokenResolver publicAwareBearerTokenResolver,
                        OncePerRequestFilter publicEndpointAuthorizationStripFilter) throws Exception {

                http
                        .csrf(AbstractHttpConfigurer::disable)
                        .cors(cors -> cors.configurationSource(corsConfigurationSource))
                        .formLogin(AbstractHttpConfigurer::disable)
                        .httpBasic(AbstractHttpConfigurer::disable)
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .addFilterBefore(publicEndpointAuthorizationStripFilter, BearerTokenAuthenticationFilter.class)
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers(publicEndpointsMatcher).permitAll()
                                .requestMatchers("/api/content/admin/**").hasRole("ADMIN")

                                .anyRequest().authenticated())
                        .oauth2ResourceServer(oauth2 -> oauth2
                                .bearerTokenResolver(publicAwareBearerTokenResolver)
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                        .exceptionHandling(ex -> ex
                                .authenticationEntryPoint((request, response, authException) -> {
                                        response.setStatus(401);
                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                        response.getWriter().write(
                                                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or missing token\"}");
                                })
                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                        response.setStatus(403);
                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                        response.getWriter().write(
                                                "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                                }));

                return http.build();
        }

        @Bean
        public BearerTokenResolver publicAwareBearerTokenResolver(RequestMatcher publicEndpointsMatcher) {
                DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

                return request -> {
                        if (publicEndpointsMatcher.matches(request)) {
                                return null;
                        }

                        return defaultResolver.resolve(request);
                };
        }

        @Bean
        public OncePerRequestFilter publicEndpointAuthorizationStripFilter(RequestMatcher publicEndpointsMatcher) {
                return new OncePerRequestFilter() {
                        @Override
                        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
                                if (!publicEndpointsMatcher.matches(request)
                                                || request.getHeader(HttpHeaders.AUTHORIZATION) == null) {
                                        filterChain.doFilter(request, response);
                                        return;
                                }

                                HttpServletRequestWrapper sanitizedRequest = new HttpServletRequestWrapper(request) {
                                        @Override
                                        public String getHeader(String name) {
                                                if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                                                        return null;
                                                }
                                                return super.getHeader(name);
                                        }

                                        @Override
                                        public Enumeration<String> getHeaders(String name) {
                                                if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                                                        return Collections.emptyEnumeration();
                                                }
                                                return super.getHeaders(name);
                                        }

                                        @Override
                                        public Enumeration<String> getHeaderNames() {
                                                List<String> headerNames = Collections.list(super.getHeaderNames());
                                                return Collections.enumeration(headerNames.stream()
                                                                .filter(name -> !HttpHeaders.AUTHORIZATION
                                                                                .equalsIgnoreCase(name))
                                                                .toList());
                                        }
                                };

                                filterChain.doFilter(sanitizedRequest, response);
                        }
                };
        }

        @Bean
        public RequestMatcher publicEndpointsMatcher() {
                return new OrRequestMatcher(
                        new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name()),
                        new AntPathRequestMatcher("/actuator/health"),
                        new AntPathRequestMatcher("/actuator/info"),
                        new AntPathRequestMatcher("/api/content/public/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/posts", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/posts/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/questions", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/questions/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/tags", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/tags/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/public/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/posts", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/posts/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/questions", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/questions/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/tags", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/tags/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/posts/*/view", HttpMethod.POST.name()),
                        new AntPathRequestMatcher("/posts/*/view", HttpMethod.POST.name()));
        }
}
