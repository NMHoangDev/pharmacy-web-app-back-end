package com.backend.gateway.filter;

import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Ensures every request has a correlation-id header for tracing across
 * services.
 */
@Component
public class RequestCorrelationFilter implements WebFilter {
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String headerValue = request.getHeaders().getFirst(HEADER_CORRELATION_ID);
        String correlationId = (headerValue == null || headerValue.isBlank())
                ? UUID.randomUUID().toString()
                : headerValue;

        ServerWebExchange mutatedExchange = exchange;
        if (headerValue == null || headerValue.isBlank()) {
            mutatedExchange = exchange.mutate()
                    .request(builder -> builder.header(HEADER_CORRELATION_ID, correlationId))
                    .build();
        }

        MDC.put(HEADER_CORRELATION_ID, correlationId);
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> MDC.remove(HEADER_CORRELATION_ID))
                .contextWrite(ctx -> ctx.put(HEADER_CORRELATION_ID, correlationId));
    }
}
