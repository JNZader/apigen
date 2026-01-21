package com.jnzader.apigen.core.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API versioning configuration.
 *
 * <p>Automatically adds the /api/v1 prefix to all controllers in the controllers package.
 *
 * <p>Supported versioning strategies: - URL path: /api/v1/resource (implemented) - Header:
 * Accept-Version: v1 (can be added) - Query param: ?version=1 (can be added)
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    @Value("${app.api.version:v1}")
    private String apiVersion;

    @Value("${app.api.base-path:/api}")
    private String basePath;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                basePath + "/" + apiVersion,
                clazz -> clazz.getPackageName().startsWith("com.jnzader.apigen.core.controller"));
    }
}
