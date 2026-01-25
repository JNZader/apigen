package com.jnzader.apigen.core.infrastructure.versioning;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves API version from incoming HTTP requests.
 *
 * <p>Supports multiple resolution strategies with configurable fallback order.
 */
public class ApiVersionResolver {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionResolver.class);

    private static final String DEFAULT_VERSION_HEADER = "X-API-Version";
    private static final String ACCEPT_VERSION_HEADER = "Accept-Version";
    private static final String API_VERSION_HEADER = "API-Version";
    private static final String DEFAULT_VERSION_PARAM = "version";
    private static final String DEFAULT_PATH_PREFIX = "v";

    private final List<VersioningStrategy> strategies;
    private final String defaultVersion;
    private final String versionHeader;
    private final String versionParam;
    private final String pathPrefix;
    private final String vendorName;
    private final Pattern pathVersionPattern;
    private final Pattern mediaTypeVersionPattern;

    @SuppressWarnings("java:S1075") // "/" is correct for URL paths (not file system paths)
    private ApiVersionResolver(Builder builder) {
        this.strategies = builder.strategies;
        this.defaultVersion = builder.defaultVersion;
        this.versionHeader = builder.versionHeader;
        this.versionParam = builder.versionParam;
        this.pathPrefix = builder.pathPrefix;
        this.vendorName = builder.vendorName;
        // Use atomic groups/possessive quantifiers where safe to prevent ReDoS attacks
        this.pathVersionPattern =
                Pattern.compile("/" + Pattern.quote(pathPrefix) + "([\\d.]++[-\\w]*+)/");
        // Build media type pattern for Accept header versioning (uses vendor name or wildcard)
        String vendorPattern =
                vendorName != null
                        ? Pattern.quote(vendorName)
                        : "[\\w.-]+?"; // fallback to any vendor
        this.mediaTypeVersionPattern =
                Pattern.compile(
                        "application/vnd\\." + vendorPattern + "\\.v([\\d.]++[-\\w]*+)\\+\\w++");
    }

    /**
     * Resolves the API version from the request using configured strategies.
     *
     * @param request the HTTP request
     * @return the resolved version, or default version if none found
     */
    public String resolve(HttpServletRequest request) {
        for (VersioningStrategy strategy : strategies) {
            Optional<String> version = resolveByStrategy(request, strategy);
            if (version.isPresent()) {
                log.debug("Resolved API version '{}' using strategy {}", version.get(), strategy);
                return version.get();
            }
        }
        log.debug("No version found, using default: {}", defaultVersion);
        return defaultVersion;
    }

    /**
     * Resolves version using a specific strategy.
     *
     * @param request the HTTP request
     * @param strategy the strategy to use
     * @return the version if found
     */
    public Optional<String> resolveByStrategy(
            HttpServletRequest request, VersioningStrategy strategy) {
        return switch (strategy) {
            case HEADER -> resolveFromHeader(request);
            case PATH -> resolveFromPath(request);
            case QUERY_PARAM -> resolveFromQueryParam(request);
            case MEDIA_TYPE -> resolveFromMediaType(request);
        };
    }

    private Optional<String> resolveFromHeader(HttpServletRequest request) {
        // Try multiple header names
        String version = request.getHeader(versionHeader);
        if (version == null) {
            version = request.getHeader(DEFAULT_VERSION_HEADER);
        }
        if (version == null) {
            version = request.getHeader(ACCEPT_VERSION_HEADER);
        }
        if (version == null) {
            version = request.getHeader(API_VERSION_HEADER);
        }
        return Optional.ofNullable(version).map(String::trim).filter(v -> !v.isEmpty());
    }

    private Optional<String> resolveFromPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        Matcher matcher = pathVersionPattern.matcher(path);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private Optional<String> resolveFromQueryParam(HttpServletRequest request) {
        String version = request.getParameter(versionParam);
        return Optional.ofNullable(version).map(String::trim).filter(v -> !v.isEmpty());
    }

    private Optional<String> resolveFromMediaType(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept == null) {
            return Optional.empty();
        }
        Matcher matcher = mediaTypeVersionPattern.matcher(accept);
        if (matcher.find()) {
            return Optional.of(matcher.group(1)); // group(1) is version
        }
        return Optional.empty();
    }

    /**
     * Returns the configured vendor name for media type versioning.
     *
     * @return the vendor name
     */
    public String getVendorName() {
        return vendorName;
    }

    /**
     * Returns the default version.
     *
     * @return the default version
     */
    public String getDefaultVersion() {
        return defaultVersion;
    }

    /**
     * Generates a media type string for the given version and format.
     *
     * @param version the API version
     * @param format the format (e.g., "json", "xml")
     * @return the media type string
     */
    public String generateMediaType(String version, String format) {
        return String.format("application/vnd.%s.v%s+%s", vendorName, version, format);
    }

    /**
     * Checks if a version string matches the expected version.
     *
     * @param requestVersion the version from the request
     * @param expectedVersion the expected version
     * @return true if versions match
     */
    public boolean matches(String requestVersion, String expectedVersion) {
        if (requestVersion == null || expectedVersion == null) {
            return false;
        }
        // Exact match
        if (requestVersion.equals(expectedVersion)) {
            return true;
        }
        // Major version match (e.g., "1" matches "1.0")
        String[] requestParts = requestVersion.split("\\.");
        String[] expectedParts = expectedVersion.split("\\.");
        if (requestParts.length == 0 || expectedParts.length == 0) {
            return false;
        }
        return requestParts[0].equals(expectedParts[0]);
    }

    /** Creates a new builder for ApiVersionResolver. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for ApiVersionResolver. */
    public static class Builder {
        private List<VersioningStrategy> strategies = List.of(VersioningStrategy.HEADER);
        private String defaultVersion = "1.0";
        private String versionHeader = DEFAULT_VERSION_HEADER;
        private String versionParam = DEFAULT_VERSION_PARAM;
        private String pathPrefix = DEFAULT_PATH_PREFIX;
        private String vendorName = "apigen";

        public Builder strategies(List<VersioningStrategy> strategies) {
            this.strategies = strategies;
            return this;
        }

        public Builder strategies(VersioningStrategy... strategies) {
            this.strategies = List.of(strategies);
            return this;
        }

        public Builder defaultVersion(String defaultVersion) {
            this.defaultVersion = defaultVersion;
            return this;
        }

        public Builder versionHeader(String versionHeader) {
            this.versionHeader = versionHeader;
            return this;
        }

        public Builder versionParam(String versionParam) {
            this.versionParam = versionParam;
            return this;
        }

        public Builder pathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
            return this;
        }

        public Builder vendorName(String vendorName) {
            this.vendorName = vendorName;
            return this;
        }

        public ApiVersionResolver build() {
            return new ApiVersionResolver(this);
        }
    }
}
