package com.backend.adminbff.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
public class AuditFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuditFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String rid = UUID.randomUUID().toString();
        request.setAttribute("X-Request-Id", rid);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long took = System.currentTimeMillis() - start;
            String actor = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
            log.info("audit reqId={} actor={} method={} path={} status={} tookMs={} at={}",
                    rid, actor, request.getMethod(), request.getRequestURI(), response.getStatus(), took,
                    Instant.now());
        }
    }
}
