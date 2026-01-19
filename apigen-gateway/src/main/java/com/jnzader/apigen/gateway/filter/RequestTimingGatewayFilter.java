package com.jnzader.apigen.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter for collecting request timing metrics. Integrates with Micrometer for
 * Prometheus/Grafana monitoring.
 */
public class RequestTimingGatewayFilter implements GlobalFilter, Ordered {

    private static final String TIMER_NAME = "gateway.requests";
    private static final String COUNTER_NAME = "gateway.requests.total";

    private final MeterRegistry meterRegistry;

    public RequestTimingGatewayFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.nanoTime();

        return chain.filter(exchange)
                .doFinally(
                        signalType -> {
                            long duration = System.nanoTime() - startTime;
                            recordMetrics(exchange, duration);
                        });
    }

    private void recordMetrics(ServerWebExchange exchange, long durationNanos) {
        String method = exchange.getRequest().getMethod().name();
        String path = normalizePath(exchange.getRequest().getURI().getPath());
        String routeId = getRouteId(exchange);
        String status = getStatusGroup(exchange.getResponse().getStatusCode());

        // Record timer
        Timer.builder(TIMER_NAME)
                .tag("method", method)
                .tag("path", path)
                .tag("route", routeId)
                .tag("status", status)
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);

        // Record counter
        Counter.builder(COUNTER_NAME)
                .tag("method", method)
                .tag("path", path)
                .tag("route", routeId)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    private String getRouteId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : "unknown";
    }

    private String getStatusGroup(HttpStatusCode statusCode) {
        if (statusCode == null) {
            return "unknown";
        }
        int code = statusCode.value();
        if (code >= 200 && code < 300) {
            return "2xx";
        } else if (code >= 300 && code < 400) {
            return "3xx";
        } else if (code >= 400 && code < 500) {
            return "4xx";
        } else if (code >= 500) {
            return "5xx";
        }
        return "other";
    }

    private String normalizePath(String path) {
        // Normalize path by replacing IDs with placeholders
        return path.replaceAll(
                        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                        "{uuid}")
                .replaceAll("/\\d+", "/{id}");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    /** Creates a builder for timing assertions in tests. */
    public static TimingAssertion assertTiming() {
        return new TimingAssertion();
    }

    /** Utility for timing assertions in tests. */
    public static class TimingAssertion {
        private Duration maxDuration;

        public TimingAssertion maxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
            return this;
        }

        public boolean check(long durationNanos) {
            if (maxDuration == null) {
                return true;
            }
            return durationNanos <= maxDuration.toNanos();
        }
    }
}
