package com.jnzader.apigen.core.infrastructure.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * ETag configuration for HTTP caching.
 *
 * <p>ShallowEtagHeaderFilter calculates an MD5 hash of the response content and uses it as an ETag.
 * This allows clients to use If-None-Match to validate their cache and receive 304 Not Modified if
 * the content has not changed.
 *
 * <p>Benefits: - Reduces bandwidth (body is not sent if unchanged) - Improves perceived response
 * time - Enables client-side cache validation
 *
 * <p>Limitations: - The server still processes the request completely (only saves bandwidth) - For
 * true processing savings, use server-side caching
 */
@Configuration
public class ETagConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());

        // Apply to all API endpoints
        filterRegistrationBean.addUrlPatterns("/api/*");

        // Set order (after logging, before rate limiting)
        filterRegistrationBean.setOrder(2);

        // Name for identification in logs
        filterRegistrationBean.setName("etagFilter");

        return filterRegistrationBean;
    }
}
