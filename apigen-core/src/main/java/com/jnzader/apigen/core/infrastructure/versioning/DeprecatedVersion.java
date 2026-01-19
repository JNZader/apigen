package com.jnzader.apigen.core.infrastructure.versioning;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an API version as deprecated with optional sunset date.
 *
 * <p>When applied, the API will:
 *
 * <ul>
 *   <li>Add a {@code Deprecation} header to responses
 *   <li>Add a {@code Sunset} header if sunset date is specified
 *   <li>Add a {@code Link} header pointing to the newer version
 *   <li>Log deprecation warnings
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/products")
 * @ApiVersion("1.0")
 * @DeprecatedVersion(
 *     since = "2024-01-01",
 *     sunset = "2025-01-01",
 *     successor = "2.0",
 *     message = "Use v2 for improved performance"
 * )
 * public class ProductControllerV1 {
 *     // Deprecated endpoints
 * }
 * }</pre>
 *
 * @see ApiVersion
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeprecatedVersion {

    /**
     * Date when this version was deprecated (ISO 8601 format: yyyy-MM-dd).
     *
     * @return the deprecation date
     */
    String since();

    /**
     * Date when this version will be removed (ISO 8601 format: yyyy-MM-dd). Used to generate the
     * Sunset HTTP header.
     *
     * @return the sunset date, or empty string if no sunset date is set
     */
    String sunset() default "";

    /**
     * The version that should be used instead.
     *
     * @return the successor version, or empty string if none
     */
    String successor() default "";

    /**
     * Optional message explaining the deprecation reason or migration path.
     *
     * @return the deprecation message
     */
    String message() default "";

    /**
     * Link to migration documentation.
     *
     * @return the migration documentation URL, or empty string if none
     */
    String migrationGuide() default "";
}
