package com.backend.content.config;

import com.backend.common.security.CommonSecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(CommonSecurityConfig.class)
public class SecurityConfig {

        private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

        private static final RequestMatcher PUBLIC_GET_ENDPOINTS = new OrRequestMatcher(
                        new AntPathRequestMatcher("/api/content/posts", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/posts/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/questions", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/questions/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/tags", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/content/tags/**", HttpMethod.GET.name()));

        private static final AuthenticationEntryPoint BEARER_ENTRY_POINT = new BearerTokenAuthenticationEntryPoint();

        // Two chains are required so that public reads are handled before the JWT
        // chain.
        // If oauth2ResourceServer is active on the public chain,
        // BearerTokenAuthenticationFilter
        // still executes and can reject missing/invalid tokens before permitAll is
        // applied.
        @Bean
        @Order(1)
        public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {

                http
                                .securityMatcher(PUBLIC_GET_ENDPOINTS)

                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)

                                .authorizeHttpRequests(auth -> auth
                                                .anyRequest().permitAll())

                                .oauth2ResourceServer(oauth2 -> oauth2.disable());

                return http.build();
        }

        // 🔐 PRIVATE API
        @Bean
        @Order(2)
        public SecurityFilterChain privateFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

                http
                                .securityMatcher("/**")
                                .csrf(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)

                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                                                .anyRequest().authenticated())

                                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response,
                                                authException) -> {
                                        Authentication authentication = SecurityContextHolder.getContext()
                                                        .getAuthentication();
                                        boolean hasAuthorizationHeader = request
                                                        .getHeader(HttpHeaders.AUTHORIZATION) != null;
                                        log.warn("[SECURITY 401] uri={} hasAuthorizationHeader={} authentication={}",
                                                        request.getRequestURI(),
                                                        hasAuthorizationHeader,
                                                        authentication == null ? "null"
                                                                        : authentication.getClass().getSimpleName() +
                                                                                        ":" +
                                                                                        authentication.isAuthenticated());
                                        BEARER_ENTRY_POINT.commence(request, response, authException);
                                }))

                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter)));

                return http.build();
        }
}