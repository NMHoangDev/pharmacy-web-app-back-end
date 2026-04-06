package com.backend.identity.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(request.getMethod()).append(" ").append(request.getRequestURI());
            if (request.getQueryString() != null)
                sb.append('?').append(request.getQueryString());
            sb.append(" from ").append(request.getRemoteAddr());
            sb.append(" headers=").append(Collections.list(request.getHeaderNames()).stream()
                    .map(name -> name + ":" + request.getHeader(name)).reduce((a, b) -> a + ";" + b).orElse(""));
            log.info("Incoming request: {}", sb.toString());
        } catch (Exception e) {
            log.warn("Failed to log request", e);
        }
        filterChain.doFilter(request, response);
    }
}
