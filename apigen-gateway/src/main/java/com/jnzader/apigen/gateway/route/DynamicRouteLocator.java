package com.jnzader.apigen.gateway.route;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

/**
 * Dynamic route locator that supports runtime route modifications. Routes can be added, updated, or
 * removed without application restart.
 */
public class DynamicRouteLocator implements RouteLocator {

    private static final Logger log = LoggerFactory.getLogger(DynamicRouteLocator.class);

    private final RouteLocatorBuilder routeLocatorBuilder;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, RouteBuilder.RouteDefinition> routeDefinitions;

    public DynamicRouteLocator(
            RouteLocatorBuilder routeLocatorBuilder, ApplicationEventPublisher eventPublisher) {
        this.routeLocatorBuilder = routeLocatorBuilder;
        this.eventPublisher = eventPublisher;
        this.routeDefinitions = new ConcurrentHashMap<>();
    }

    @Override
    public Flux<Route> getRoutes() {
        if (routeDefinitions.isEmpty()) {
            return Flux.empty();
        }

        RouteLocatorBuilder.Builder builder = routeLocatorBuilder.routes();

        for (RouteBuilder.RouteDefinition definition : routeDefinitions.values()) {
            builder = builder.route(definition.id(), r -> buildRoute(r, definition));
        }

        return builder.build().getRoutes();
    }

    private org.springframework.cloud.gateway.route.builder.Buildable<Route> buildRoute(
            org.springframework.cloud.gateway.route.builder.PredicateSpec r,
            RouteBuilder.RouteDefinition definition) {
        var predicateSpec = r.path(definition.pathPredicates().toArray(new String[0]));
        predicateSpec = applyMethodPredicates(predicateSpec, definition);
        predicateSpec = applyHeaderPredicates(predicateSpec, definition);
        return predicateSpec.filters(f -> applyFilters(f, definition)).uri(definition.uri());
    }

    private org.springframework.cloud.gateway.route.builder.BooleanSpec applyMethodPredicates(
            org.springframework.cloud.gateway.route.builder.BooleanSpec predicateSpec,
            RouteBuilder.RouteDefinition definition) {
        if (!definition.hasMethodPredicates()) {
            return predicateSpec;
        }
        for (String method : definition.methodPredicates()) {
            predicateSpec = predicateSpec.and().method(method);
        }
        return predicateSpec;
    }

    private org.springframework.cloud.gateway.route.builder.BooleanSpec applyHeaderPredicates(
            org.springframework.cloud.gateway.route.builder.BooleanSpec predicateSpec,
            RouteBuilder.RouteDefinition definition) {
        for (String header : definition.headerPredicates()) {
            String[] parts = header.split(",", 2);
            predicateSpec =
                    parts.length == 2
                            ? predicateSpec.and().header(parts[0], parts[1])
                            : predicateSpec.and().header(parts[0]);
        }
        return predicateSpec;
    }

    private org.springframework.cloud.gateway.route.builder.GatewayFilterSpec applyFilters(
            org.springframework.cloud.gateway.route.builder.GatewayFilterSpec spec,
            RouteBuilder.RouteDefinition definition) {
        spec = applyStripPrefix(spec, definition);
        spec = applyRewritePath(spec, definition);
        spec = applyRequestHeader(spec, definition);
        spec = applyCircuitBreaker(spec, definition);
        return applyCustomFilters(spec, definition);
    }

    private org.springframework.cloud.gateway.route.builder.GatewayFilterSpec applyStripPrefix(
            org.springframework.cloud.gateway.route.builder.GatewayFilterSpec spec,
            RouteBuilder.RouteDefinition definition) {
        return definition.stripPrefix() ? spec.stripPrefix(definition.stripPrefixParts()) : spec;
    }

    private org.springframework.cloud.gateway.route.builder.GatewayFilterSpec applyRewritePath(
            org.springframework.cloud.gateway.route.builder.GatewayFilterSpec spec,
            RouteBuilder.RouteDefinition definition) {
        return definition.hasRewritePath()
                ? spec.rewritePath(definition.rewritePath(), definition.rewritePathReplacement())
                : spec;
    }

    private org.springframework.cloud.gateway.route.builder.GatewayFilterSpec applyRequestHeader(
            org.springframework.cloud.gateway.route.builder.GatewayFilterSpec spec,
            RouteBuilder.RouteDefinition definition) {
        return definition.addRequestHeader()
                ? spec.addRequestHeader(
                        definition.requestHeaderName(), definition.requestHeaderValue())
                : spec;
    }

    private org.springframework.cloud.gateway.route.builder.GatewayFilterSpec applyCircuitBreaker(
            org.springframework.cloud.gateway.route.builder.GatewayFilterSpec spec,
            RouteBuilder.RouteDefinition definition) {
        return definition.hasCircuitBreaker()
                ? spec.circuitBreaker(config -> config.setName(definition.circuitBreakerId()))
                : spec;
    }

    private org.springframework.cloud.gateway.route.builder.GatewayFilterSpec applyCustomFilters(
            org.springframework.cloud.gateway.route.builder.GatewayFilterSpec spec,
            RouteBuilder.RouteDefinition definition) {
        for (var filter : definition.filters()) {
            spec = spec.filter(filter);
        }
        return spec;
    }

    /** Adds a new route dynamically. */
    public void addRoute(RouteBuilder.RouteDefinition definition) {
        log.info("Adding dynamic route: {}", definition.id());
        routeDefinitions.put(definition.id(), definition);
        refreshRoutes();
    }

    /** Updates an existing route. */
    public void updateRoute(RouteBuilder.RouteDefinition definition) {
        if (!routeDefinitions.containsKey(definition.id())) {
            log.warn("Route {} not found for update, adding as new", definition.id());
        } else {
            log.info("Updating dynamic route: {}", definition.id());
        }
        routeDefinitions.put(definition.id(), definition);
        refreshRoutes();
    }

    /** Removes a route by ID. */
    public boolean removeRoute(String routeId) {
        RouteBuilder.RouteDefinition removed = routeDefinitions.remove(routeId);
        if (removed != null) {
            log.info("Removed dynamic route: {}", routeId);
            refreshRoutes();
            return true;
        }
        log.warn("Route {} not found for removal", routeId);
        return false;
    }

    /** Gets all route definitions. */
    public List<RouteBuilder.RouteDefinition> getRouteDefinitions() {
        return List.copyOf(routeDefinitions.values());
    }

    /** Checks if a route exists. */
    public boolean hasRoute(String routeId) {
        return routeDefinitions.containsKey(routeId);
    }

    /** Clears all dynamic routes. */
    public void clearRoutes() {
        log.info("Clearing all dynamic routes");
        routeDefinitions.clear();
        refreshRoutes();
    }

    private void refreshRoutes() {
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
