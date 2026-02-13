package com.backend.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayRouteLogger implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(GatewayRouteLogger.class);

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        Route route = exchange
                .getAttribute(org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "none";
        String target = route != null && route.getUri() != null ? route.getUri().toString() : "unknown";
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        String authPresent = (auth != null && !auth.isEmpty()) ? "yes" : "no";
        String authMasked = "none";
        if (auth != null && !auth.isEmpty()) {
            if (auth.length() > 30) {
                authMasked = auth.substring(0, 7) + "..." + auth.substring(auth.length() - 10);
            } else {
                authMasked = auth;
            }
        }
        log.info("GATEWAY incoming: method={} path={} routeId={} target={} authPresent={} authMasked={}", method, path,
                routeId, target, authPresent, authMasked);
        return chain.filter(exchange);
    }
}
