package com.backend.gateway.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public ResponseEntity<Map<String, Object>> fallback(ServerWebExchange exchange) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "SERVICE_UNAVAILABLE");
        body.put("message", "Gateway fallback: downstream service unavailable or timed out");
        body.put("path", exchange.getRequest().getPath().value());

        var method = exchange.getRequest().getMethod();
        body.put("method", method != null ? method.name() : "UNKNOWN");
        body.put("code", "GATEWAY_FALLBACK");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body);
    }
}
