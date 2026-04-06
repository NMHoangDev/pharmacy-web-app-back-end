package com.backend.appointment.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor authHeaderForwardingInterceptor() {
        return requestTemplate -> {
            String authorization = resolveAuthorizationHeader();
            if (StringUtils.hasText(authorization)) {
                requestTemplate.header(HttpHeaders.AUTHORIZATION, authorization);
            }
        };
    }

    private String resolveAuthorizationHeader() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            if (request != null) {
                String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (StringUtils.hasText(header)) {
                    return header;
                }
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String tokenValue = jwtAuthenticationToken.getToken().getTokenValue();
            if (StringUtils.hasText(tokenValue)) {
                return "Bearer " + tokenValue;
            }
        }

        Object credentials = authentication == null ? null : authentication.getCredentials();
        if (credentials instanceof String credentialText && StringUtils.hasText(credentialText)) {
            return credentialText.startsWith("Bearer ") ? credentialText : "Bearer " + credentialText;
        }

        return null;
    }
}
