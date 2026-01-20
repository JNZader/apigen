package com.jnzader.apigen.core.infrastructure.versioning;

/**
 * Strategies for API version resolution.
 *
 * <p>Multiple strategies can be combined for fallback behavior.
 */
public enum VersioningStrategy {

    /**
     * Version in URL path: {@code /api/v1/products}
     *
     * <p>The version prefix is configurable (default: "v").
     */
    PATH,

    /**
     * Version in HTTP header.
     *
     * <p>Supports multiple header formats:
     *
     * <ul>
     *   <li>{@code X-API-Version: 1.0}
     *   <li>{@code Accept-Version: 1.0}
     *   <li>{@code API-Version: 1.0}
     * </ul>
     */
    HEADER,

    /**
     * Version in query parameter: {@code /api/products?version=1.0}
     *
     * <p>The parameter name is configurable (default: "version").
     */
    QUERY_PARAM,

    /**
     * Version in Accept header media type: {@code Accept: application/vnd.api.v1+json}
     *
     * <p>Follows content negotiation best practices.
     */
    MEDIA_TYPE
}
