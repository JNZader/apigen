package com.jnzader.apigen.core.infrastructure.versioning;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for API versioning.
 *
 * <p>Enable with:
 *
 * <pre>{@code
 * apigen.versioning.enabled=true
 * }</pre>
 *
 * <p>Configuration options:
 *
 * <pre>{@code
 * apigen:
 *   versioning:
 *     enabled: true
 *     default-version: "1.0"
 *     strategies:
 *       - HEADER
 *       - PATH
 *     header-name: "X-API-Version"
 *     query-param: "version"
 *     path-prefix: "v"
 * }</pre>
 */
@Configuration
@ConditionalOnProperty(prefix = "apigen.versioning", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ApiVersionAutoConfiguration.VersioningProperties.class)
public class ApiVersionAutoConfiguration implements WebMvcConfigurer {

    private final VersioningProperties properties;

    public ApiVersionAutoConfiguration(VersioningProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ApiVersionResolver apiVersionResolver() {
        return ApiVersionResolver.builder()
                .strategies(properties.getStrategies())
                .defaultVersion(properties.getDefaultVersion())
                .versionHeader(properties.getHeaderName())
                .versionParam(properties.getQueryParam())
                .pathPrefix(properties.getPathPrefix())
                .vendorName(properties.getVendorName())
                .build();
    }

    @Bean
    public ApiVersionInterceptor apiVersionInterceptor(ApiVersionResolver versionResolver) {
        return new ApiVersionInterceptor(versionResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiVersionInterceptor(apiVersionResolver()))
                .addPathPatterns("/api/**");
    }

    /** Configuration properties for API versioning. */
    @ConfigurationProperties(prefix = "apigen.versioning")
    public static class VersioningProperties {

        /** Whether API versioning is enabled. Default: false. */
        private boolean enabled = false;

        /** Default API version when none is specified. Default: "1.0". */
        private String defaultVersion = "1.0";

        /** Versioning strategies to use, in order of precedence. Default: [HEADER]. */
        private List<VersioningStrategy> strategies = List.of(VersioningStrategy.HEADER);

        /** Header name for version. Default: "X-API-Version". */
        private String headerName = "X-API-Version";

        /** Query parameter name for version. Default: "version". */
        private String queryParam = "version";

        /** Path prefix for version. Default: "v". */
        private String pathPrefix = "v";

        /** Vendor name for media type versioning. Default: "apigen". */
        private String vendorName = "apigen";

        /** Supported API versions. Default: ["1.0"]. */
        private List<String> supportedVersions = List.of("1.0");

        // Getters and setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDefaultVersion() {
            return defaultVersion;
        }

        public void setDefaultVersion(String defaultVersion) {
            this.defaultVersion = defaultVersion;
        }

        public List<VersioningStrategy> getStrategies() {
            return strategies;
        }

        public void setStrategies(List<VersioningStrategy> strategies) {
            this.strategies = strategies;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public String getQueryParam() {
            return queryParam;
        }

        public void setQueryParam(String queryParam) {
            this.queryParam = queryParam;
        }

        public String getPathPrefix() {
            return pathPrefix;
        }

        public void setPathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }

        public String getVendorName() {
            return vendorName;
        }

        public void setVendorName(String vendorName) {
            this.vendorName = vendorName;
        }

        public List<String> getSupportedVersions() {
            return supportedVersions;
        }

        public void setSupportedVersions(List<String> supportedVersions) {
            this.supportedVersions = supportedVersions;
        }
    }
}
