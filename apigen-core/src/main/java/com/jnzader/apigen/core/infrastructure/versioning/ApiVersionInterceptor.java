package com.jnzader.apigen.core.infrastructure.versioning;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that handles API versioning and deprecation headers.
 *
 * <p>Adds the following headers to responses:
 *
 * <ul>
 *   <li>{@code X-API-Version} - The resolved API version
 *   <li>{@code Deprecation} - If the endpoint is deprecated (RFC 8594)
 *   <li>{@code Sunset} - When the endpoint will be removed (RFC 8594)
 *   <li>{@code Link} - Link to successor version or migration guide
 * </ul>
 */
public class ApiVersionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionInterceptor.class);

    private static final String VERSION_ATTRIBUTE = "apigen.api.version";
    private static final String VERSION_HEADER = "X-API-Version";
    private static final String DEPRECATION_HEADER = "Deprecation";
    private static final String SUNSET_HEADER = "Sunset";
    private static final String LINK_HEADER = "Link";

    private static final DateTimeFormatter HTTP_DATE_FORMAT =
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ApiVersionResolver versionResolver;

    public ApiVersionInterceptor(ApiVersionResolver versionResolver) {
        this.versionResolver = versionResolver;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {

        // Resolve version and store in request attribute
        String version = versionResolver.resolve(request);
        request.setAttribute(VERSION_ATTRIBUTE, version);

        // Add version header to response
        response.setHeader(VERSION_HEADER, version);

        // Handle deprecation if handler is a method
        if (handler instanceof HandlerMethod handlerMethod) {
            handleDeprecation(request, response, handlerMethod);
        }

        return true;
    }

    private void handleDeprecation(
            HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) {

        // Check method-level deprecation first, then class-level
        DeprecatedVersion deprecation = handlerMethod.getMethodAnnotation(DeprecatedVersion.class);
        if (deprecation == null) {
            deprecation = handlerMethod.getBeanType().getAnnotation(DeprecatedVersion.class);
        }

        if (deprecation == null) {
            return;
        }

        // Add Deprecation header (RFC 8594)
        addDeprecationHeader(response, deprecation);

        // Add Sunset header if date is specified
        addSunsetHeader(response, deprecation);

        // Add Link header for successor or migration guide
        addLinkHeader(request, response, deprecation);

        // Log deprecation warning
        logDeprecationWarning(request, deprecation);
    }

    private void addDeprecationHeader(HttpServletResponse response, DeprecatedVersion deprecation) {
        try {
            LocalDate sinceDate = LocalDate.parse(deprecation.since(), ISO_DATE);
            String httpDate =
                    HTTP_DATE_FORMAT.format(sinceDate.atStartOfDay().atZone(ZoneOffset.UTC));
            response.setHeader(DEPRECATION_HEADER, httpDate);
        } catch (DateTimeParseException _) {
            // Use "true" as fallback per RFC 8594
            response.setHeader(DEPRECATION_HEADER, "true");
        }
    }

    private void addSunsetHeader(HttpServletResponse response, DeprecatedVersion deprecation) {
        String sunset = deprecation.sunset();
        if (sunset == null || sunset.isEmpty()) {
            return;
        }

        try {
            LocalDate sunsetDate = LocalDate.parse(sunset, ISO_DATE);
            String httpDate =
                    HTTP_DATE_FORMAT.format(sunsetDate.atStartOfDay().atZone(ZoneOffset.UTC));
            response.setHeader(SUNSET_HEADER, httpDate);
        } catch (DateTimeParseException _) {
            log.warn("Invalid sunset date format: {}", sunset);
        }
    }

    private void addLinkHeader(
            HttpServletRequest request,
            HttpServletResponse response,
            DeprecatedVersion deprecation) {

        StringBuilder linkBuilder = new StringBuilder();

        // Add successor link
        String successor = deprecation.successor();
        if (successor != null && !successor.isEmpty()) {
            String currentPath = request.getRequestURI();
            // Simple path transformation: replace version in path
            String successorPath = transformPathToVersion(currentPath, successor);
            linkBuilder.append("<").append(successorPath).append(">; rel=\"successor-version\"");
        }

        // Add migration guide link
        String migrationGuide = deprecation.migrationGuide();
        if (migrationGuide != null && !migrationGuide.isEmpty()) {
            if (!linkBuilder.isEmpty()) {
                linkBuilder.append(", ");
            }
            linkBuilder
                    .append("<")
                    .append(migrationGuide)
                    .append(">; rel=\"deprecation\"; type=\"text/html\"");
        }

        if (!linkBuilder.isEmpty()) {
            response.setHeader(LINK_HEADER, linkBuilder.toString());
        }
    }

    private String transformPathToVersion(String currentPath, String newVersion) {
        // Transform /api/v1/products to /api/v2/products
        // Use possessive quantifiers to prevent ReDoS attacks
        return currentPath.replaceFirst("/v[\\d.]++[-\\w]*+/", "/v" + newVersion + "/");
    }

    private void logDeprecationWarning(HttpServletRequest request, DeprecatedVersion deprecation) {
        String message = deprecation.message();
        String successor = deprecation.successor();
        String sunset = deprecation.sunset();

        StringBuilder warning = new StringBuilder();
        warning.append("Deprecated API endpoint accessed: ")
                .append(request.getMethod())
                .append(" ")
                .append(request.getRequestURI());

        if (!message.isEmpty()) {
            warning.append(" - ").append(message);
        }
        if (!successor.isEmpty()) {
            warning.append(" (successor: v").append(successor).append(")");
        }
        if (!sunset.isEmpty()) {
            warning.append(" [sunset: ").append(sunset).append("]");
        }

        if (log.isWarnEnabled()) {
            log.warn(warning.toString());
        }
    }

    /**
     * Gets the API version from the request attribute.
     *
     * @param request the HTTP request
     * @return the API version, or null if not set
     */
    public static String getVersion(HttpServletRequest request) {
        return (String) request.getAttribute(VERSION_ATTRIBUTE);
    }
}
