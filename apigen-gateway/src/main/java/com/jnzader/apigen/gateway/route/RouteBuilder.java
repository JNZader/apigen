package com.jnzader.apigen.gateway.route;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.gateway.filter.GatewayFilter;

/**
 * Fluent builder for creating Gateway routes programmatically. Provides a type-safe API for common
 * route configurations.
 */
public class RouteBuilder {

    private final String id;
    private URI uri;
    private final List<String> pathPredicates = new ArrayList<>();
    private final List<String> methodPredicates = new ArrayList<>();
    private final List<String> headerPredicates = new ArrayList<>();
    private final List<GatewayFilter> filters = new ArrayList<>();
    private final Map<String, String> metadata = new HashMap<>();
    private int order = 0;
    private boolean stripPrefix = false;
    private int stripPrefixParts = 1;
    private String rewritePath;
    private String rewritePathReplacement;
    private Duration timeout;
    private String circuitBreakerId;
    private boolean addRequestHeader = false;
    private String requestHeaderName;
    private String requestHeaderValue;

    private RouteBuilder(String id) {
        this.id = id;
    }

    public static RouteBuilder route(String id) {
        return new RouteBuilder(id);
    }

    public RouteBuilder uri(String uri) {
        this.uri = URI.create(uri);
        return this;
    }

    public RouteBuilder uri(URI uri) {
        this.uri = uri;
        return this;
    }

    public RouteBuilder path(String... paths) {
        for (String path : paths) {
            this.pathPredicates.add(path);
        }
        return this;
    }

    public RouteBuilder method(String... methods) {
        for (String method : methods) {
            this.methodPredicates.add(method);
        }
        return this;
    }

    public RouteBuilder header(String header, String regex) {
        this.headerPredicates.add(header + "," + regex);
        return this;
    }

    public RouteBuilder header(String header) {
        this.headerPredicates.add(header);
        return this;
    }

    public RouteBuilder filter(GatewayFilter filter) {
        this.filters.add(filter);
        return this;
    }

    public RouteBuilder metadata(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }

    public RouteBuilder order(int order) {
        this.order = order;
        return this;
    }

    public RouteBuilder stripPrefix(int parts) {
        this.stripPrefix = true;
        this.stripPrefixParts = parts;
        return this;
    }

    public RouteBuilder rewritePath(String regex, String replacement) {
        this.rewritePath = regex;
        this.rewritePathReplacement = replacement;
        return this;
    }

    public RouteBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public RouteBuilder circuitBreaker(String id) {
        this.circuitBreakerId = id;
        return this;
    }

    public RouteBuilder addRequestHeader(String name, String value) {
        this.addRequestHeader = true;
        this.requestHeaderName = name;
        this.requestHeaderValue = value;
        return this;
    }

    /** Builds the route definition for use with RouteLocatorBuilder. */
    public RouteDefinition buildDefinition() {
        return new RouteDefinition(
                id,
                uri,
                pathPredicates,
                methodPredicates,
                headerPredicates,
                filters,
                metadata,
                order,
                stripPrefix,
                stripPrefixParts,
                rewritePath,
                rewritePathReplacement,
                timeout,
                circuitBreakerId,
                addRequestHeader,
                requestHeaderName,
                requestHeaderValue);
    }

    /** Gets the route ID. */
    public String getId() {
        return id;
    }

    /** Gets the target URI. */
    public URI getUri() {
        return uri;
    }

    /** Gets the path predicates. */
    public List<String> getPathPredicates() {
        return List.copyOf(pathPredicates);
    }

    /** Gets the method predicates. */
    public List<String> getMethodPredicates() {
        return List.copyOf(methodPredicates);
    }

    /** Route definition record. */
    public record RouteDefinition(
            String id,
            URI uri,
            List<String> pathPredicates,
            List<String> methodPredicates,
            List<String> headerPredicates,
            List<GatewayFilter> filters,
            Map<String, String> metadata,
            int order,
            boolean stripPrefix,
            int stripPrefixParts,
            String rewritePath,
            String rewritePathReplacement,
            Duration timeout,
            String circuitBreakerId,
            boolean addRequestHeader,
            String requestHeaderName,
            String requestHeaderValue) {

        public boolean hasPathPredicates() {
            return !pathPredicates.isEmpty();
        }

        public boolean hasMethodPredicates() {
            return !methodPredicates.isEmpty();
        }

        public boolean hasHeaderPredicates() {
            return !headerPredicates.isEmpty();
        }

        public boolean hasFilters() {
            return !filters.isEmpty();
        }

        public boolean hasRewritePath() {
            return rewritePath != null && rewritePathReplacement != null;
        }

        public boolean hasTimeout() {
            return timeout != null;
        }

        public boolean hasCircuitBreaker() {
            return circuitBreakerId != null;
        }
    }
}
