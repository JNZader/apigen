package com.jnzader.apigen.gateway.config;

import com.jnzader.apigen.gateway.filter.AuthenticationGatewayFilter;
import com.jnzader.apigen.gateway.filter.LoggingGatewayFilter;
import com.jnzader.apigen.gateway.filter.RateLimitKeyResolver;
import com.jnzader.apigen.gateway.filter.RequestTimingGatewayFilter;
import com.jnzader.apigen.gateway.route.DynamicRouteLocator;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/** Auto-configuration for APiGen Gateway components. */
@AutoConfiguration
@ConditionalOnClass(RouteLocator.class)
@ConditionalOnProperty(prefix = "apigen.gateway", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GatewayAutoConfiguration.class);

    public GatewayAutoConfiguration() {
        log.info("APiGen Gateway auto-configuration enabled");
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "apigen.gateway.logging",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public LoggingGatewayFilter loggingGatewayFilter(GatewayProperties properties) {
        log.info("Configuring logging gateway filter");
        return new LoggingGatewayFilter(properties.getLogging().isIncludeHeaders());
    }

    @Bean
    @ConditionalOnMissingBean(name = "gatewayKeyResolver")
    @ConditionalOnProperty(
            prefix = "apigen.gateway.rate-limit",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public KeyResolver gatewayKeyResolver() {
        log.info("Configuring rate limit key resolver");
        return new RateLimitKeyResolver(RateLimitKeyResolver.KeyResolutionStrategy.IP);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public RequestTimingGatewayFilter requestTimingGatewayFilter(MeterRegistry meterRegistry) {
        log.info("Configuring request timing gateway filter");
        return new RequestTimingGatewayFilter(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicRouteLocator dynamicRouteLocator(
            RouteLocatorBuilder routeLocatorBuilder, ApplicationEventPublisher eventPublisher) {
        log.info("Configuring dynamic route locator");
        return new DynamicRouteLocator(routeLocatorBuilder, eventPublisher);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "apigen.gateway.cors",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public CorsWebFilter corsWebFilter(GatewayProperties properties) {
        log.info("Configuring CORS filter");

        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(properties.getCors().getAllowedOrigins());
        corsConfig.setAllowedMethods(properties.getCors().getAllowedMethods());
        corsConfig.setAllowedHeaders(properties.getCors().getAllowedHeaders());
        corsConfig.setAllowCredentials(properties.getCors().isAllowCredentials());
        corsConfig.setMaxAge(properties.getCors().getMaxAge().getSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    /** Creates an authentication filter bean when a token validator is provided. */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "apigen.gateway.auth",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public AuthenticationGatewayFilter authenticationGatewayFilter(
            GatewayProperties properties,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
                    Function<String, AuthenticationGatewayFilter.AuthResult> tokenValidator) {

        if (tokenValidator == null) {
            log.warn(
                    "No token validator provided, authentication filter will reject all requests."
                        + " Provide a Function<String, AuthResult> bean to enable authentication.");
            tokenValidator =
                    token ->
                            AuthenticationGatewayFilter.AuthResult.failure(
                                    "No token validator configured");
        }

        log.info("Configuring authentication gateway filter");
        return new AuthenticationGatewayFilter(
                tokenValidator,
                properties.getAuth().getExcludedPaths(),
                properties.getAuth().getHeaderName(),
                properties.getAuth().getTokenPrefix());
    }
}
