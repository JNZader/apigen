package com.jnzader.apigen.core.infrastructure.versioning;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for API version information.
 *
 * <p>Provides an endpoint to discover available API versions and their media types.
 */
@RestController
@RequestMapping("/api/versions")
@ConditionalOnProperty(prefix = "apigen.versioning", name = "enabled", havingValue = "true")
public class ApiVersionController {

    private final ApiVersionResolver versionResolver;
    private final ApiVersionAutoConfiguration.VersioningProperties properties;

    public ApiVersionController(
            ApiVersionResolver versionResolver,
            ApiVersionAutoConfiguration.VersioningProperties properties) {
        this.versionResolver = versionResolver;
        this.properties = properties;
    }

    /**
     * Returns information about available API versions.
     *
     * @return the API version information
     */
    @GetMapping
    public ResponseEntity<ApiVersionInfo> getVersionInfo() {
        List<VersionDetail> versions =
                properties.getSupportedVersions().stream()
                        .map(
                                v ->
                                        new VersionDetail(
                                                v,
                                                versionResolver.generateMediaType(v, "json"),
                                                v.equals(versionResolver.getDefaultVersion())))
                        .toList();

        ApiVersionInfo info =
                new ApiVersionInfo(
                        versionResolver.getVendorName(),
                        versionResolver.getDefaultVersion(),
                        versions,
                        new ContentNegotiation(
                                properties.getHeaderName(),
                                properties.getQueryParam(),
                                properties.getPathPrefix(),
                                "application/vnd."
                                        + versionResolver.getVendorName()
                                        + ".v{version}+json"));

        return ResponseEntity.ok(info);
    }

    /** API version information response. */
    public record ApiVersionInfo(
            String vendorName,
            String defaultVersion,
            List<VersionDetail> supportedVersions,
            ContentNegotiation contentNegotiation) {}

    /** Details about a specific API version. */
    public record VersionDetail(String version, String mediaType, boolean isDefault) {}

    /** Content negotiation options. */
    public record ContentNegotiation(
            String headerName, String queryParam, String pathPrefix, String mediaTypePattern) {}
}
