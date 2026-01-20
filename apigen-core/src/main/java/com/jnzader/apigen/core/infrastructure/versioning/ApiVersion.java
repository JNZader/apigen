package com.jnzader.apigen.core.infrastructure.versioning;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller or method with an API version.
 *
 * <p>Can be used at class level to version all endpoints in a controller, or at method level for
 * individual endpoints.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/products")
 * @ApiVersion("1.0")
 * public class ProductControllerV1 {
 *     // All endpoints are version 1.0
 * }
 *
 * @RestController
 * @RequestMapping("/api/products")
 * @ApiVersion("2.0")
 * public class ProductControllerV2 {
 *     // All endpoints are version 2.0
 * }
 * }</pre>
 *
 * @see DeprecatedVersion
 * @see VersioningStrategy
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {

    /**
     * The version string (e.g., "1.0", "2.0", "1.1-beta").
     *
     * @return the API version
     */
    String value();

    /**
     * Optional media type for content negotiation. If specified, this version will only match
     * requests with this Accept header.
     *
     * <p>Example: "application/vnd.api.v1+json"
     *
     * @return the media type, or empty string for no media type constraint
     */
    String mediaType() default "";
}
