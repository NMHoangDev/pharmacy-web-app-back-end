package com.backend.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@Component
public class RouteLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RouteLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        List<URI> uris = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String routeId = route == null ? "<no-route>" : route.getId();
        String target = (uris == null || uris.isEmpty()) ? "<no-target>" : uris.get(0).toString();
        log.debug("Gateway routing: routeId={} target={} path={}", routeId, target,
                exchange.getRequest().getURI().getPath());
        return chain.filter(exchange)
                .doOnError(err -> log.debug("Routing error for routeId={}: {}", routeId, err.getMessage()));
    }

    @Override
    public int getOrder() {
        // run early
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
