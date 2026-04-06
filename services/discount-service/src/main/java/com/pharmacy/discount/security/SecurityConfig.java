package com.pharmacy.discount.security;

import com.backend.common.security.CommonSecurityConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(CommonSecurityConfig.class)
public class SecurityConfig {

        @Value("${CORS_ALLOWED_ORIGIN_PATTERNS:${app.cors.allowed-origin-patterns:http://localhost:3000,http://192.168.56.1:3000,http://localhost:*}}")
        private String corsAllowedOriginPatterns;

        @Value("${app.security.enabled:true}")
        private boolean securityEnabled;

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

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

                if (!securityEnabled) {
                        http.authorizeHttpRequests(auth -> auth
                                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                        .anyRequest().permitAll());
                        return http.build();
                }

                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                                                .requestMatchers(new AntPathRequestMatcher("/api/ping")).permitAll()
                                                .requestMatchers(new AntPathRequestMatcher("/internal/**")).permitAll()
                                                .requestMatchers(HttpMethod.GET, "/user/discounts/campaigns")
                                                .permitAll()
                                                .requestMatchers(new AntPathRequestMatcher("/admin/**"))
                                                .hasRole("ADMIN")
                                                .requestMatchers(new AntPathRequestMatcher("/user/**")).hasRole("USER")
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(UNAUTHORIZED.value());
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        response.getWriter().write(
                                                                        "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or missing token\"}");
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setStatus(FORBIDDEN.value());
                                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                                        response.getWriter().write(
                                                                        "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Insufficient permissions\"}");
                                                }));

                return http.build();
        }
}
