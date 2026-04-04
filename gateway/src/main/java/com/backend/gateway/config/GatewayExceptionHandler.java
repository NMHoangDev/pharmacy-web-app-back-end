package com.backend.gateway.config;

import com.backend.common.security.AuthErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Instant;

@Component
@Order(-2)
public class GatewayExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof AuthenticationException) {
            return handleSecurityError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
        } else if (ex instanceof AccessDeniedException) {
            return handleSecurityError(exchange, HttpStatus.FORBIDDEN, "Forbidden",
                    "You do not have permission to access this resource");
        } else if (isUpstreamUnavailable(ex)) {
            return handleSecurityError(exchange, HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                    "Upstream service is unavailable. Make sure the target local service is running.");
        }
        return Mono.error(ex);
    }

    private boolean isUpstreamUnavailable(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException || current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Mono<Void> handleSecurityError(ServerWebExchange exchange, HttpStatus status, String error,
            String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        AuthErrorResponse errDto = new AuthErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                exchange.getRequest().getPath().value(),
                exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"));

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errDto);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
