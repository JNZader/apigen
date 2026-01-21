package com.jnzader.apigen.core.infrastructure.versioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiVersionController Tests")
class ApiVersionControllerTest {

    @Mock private ApiVersionResolver versionResolver;

    @Mock private ApiVersionAutoConfiguration.VersioningProperties properties;

    private ApiVersionController controller;

    @BeforeEach
    void setUp() {
        controller = new ApiVersionController(versionResolver, properties);
    }

    @Nested
    @DisplayName("GetVersionInfo Tests")
    class GetVersionInfoTests {

        @Test
        @DisplayName("should return version info with all supported versions")
        void shouldReturnVersionInfoWithAllSupportedVersions() {
            when(properties.getSupportedVersions()).thenReturn(List.of("1.0", "2.0"));
            when(properties.getHeaderName()).thenReturn("X-API-Version");
            when(properties.getQueryParam()).thenReturn("version");
            when(properties.getPathPrefix()).thenReturn("v");
            when(versionResolver.getVendorName()).thenReturn("apigen");
            when(versionResolver.getDefaultVersion()).thenReturn("1.0");
            when(versionResolver.generateMediaType("1.0", "json"))
                    .thenReturn("application/vnd.apigen.v1.0+json");
            when(versionResolver.generateMediaType("2.0", "json"))
                    .thenReturn("application/vnd.apigen.v2.0+json");

            ResponseEntity<ApiVersionController.ApiVersionInfo> response =
                    controller.getVersionInfo();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().vendorName()).isEqualTo("apigen");
            assertThat(response.getBody().defaultVersion()).isEqualTo("1.0");
            assertThat(response.getBody().supportedVersions()).hasSize(2);
        }

        @Test
        @DisplayName("should mark default version correctly")
        void shouldMarkDefaultVersionCorrectly() {
            when(properties.getSupportedVersions()).thenReturn(List.of("1.0", "2.0"));
            when(properties.getHeaderName()).thenReturn("X-API-Version");
            when(properties.getQueryParam()).thenReturn("version");
            when(properties.getPathPrefix()).thenReturn("v");
            when(versionResolver.getVendorName()).thenReturn("apigen");
            when(versionResolver.getDefaultVersion()).thenReturn("2.0");
            when(versionResolver.generateMediaType("1.0", "json"))
                    .thenReturn("application/vnd.apigen.v1.0+json");
            when(versionResolver.generateMediaType("2.0", "json"))
                    .thenReturn("application/vnd.apigen.v2.0+json");

            ResponseEntity<ApiVersionController.ApiVersionInfo> response =
                    controller.getVersionInfo();

            List<ApiVersionController.VersionDetail> versions =
                    response.getBody().supportedVersions();
            assertThat(
                            versions.stream()
                                    .filter(v -> v.version().equals("1.0"))
                                    .findFirst()
                                    .get()
                                    .isDefault())
                    .isFalse();
            assertThat(
                            versions.stream()
                                    .filter(v -> v.version().equals("2.0"))
                                    .findFirst()
                                    .get()
                                    .isDefault())
                    .isTrue();
        }

        @Test
        @DisplayName("should include content negotiation options")
        void shouldIncludeContentNegotiationOptions() {
            when(properties.getSupportedVersions()).thenReturn(List.of("1.0"));
            when(properties.getHeaderName()).thenReturn("X-API-Version");
            when(properties.getQueryParam()).thenReturn("api_version");
            when(properties.getPathPrefix()).thenReturn("version");
            when(versionResolver.getVendorName()).thenReturn("myapi");
            when(versionResolver.getDefaultVersion()).thenReturn("1.0");
            when(versionResolver.generateMediaType("1.0", "json"))
                    .thenReturn("application/vnd.myapi.v1.0+json");

            ResponseEntity<ApiVersionController.ApiVersionInfo> response =
                    controller.getVersionInfo();

            ApiVersionController.ContentNegotiation contentNegotiation =
                    response.getBody().contentNegotiation();
            assertThat(contentNegotiation.headerName()).isEqualTo("X-API-Version");
            assertThat(contentNegotiation.queryParam()).isEqualTo("api_version");
            assertThat(contentNegotiation.pathPrefix()).isEqualTo("version");
            assertThat(contentNegotiation.mediaTypePattern())
                    .isEqualTo("application/vnd.myapi.v{version}+json");
        }

        @Test
        @DisplayName("should generate correct media type for each version")
        void shouldGenerateCorrectMediaTypeForEachVersion() {
            when(properties.getSupportedVersions()).thenReturn(List.of("1.0", "2.0", "3.0"));
            when(properties.getHeaderName()).thenReturn("X-API-Version");
            when(properties.getQueryParam()).thenReturn("version");
            when(properties.getPathPrefix()).thenReturn("v");
            when(versionResolver.getVendorName()).thenReturn("apigen");
            when(versionResolver.getDefaultVersion()).thenReturn("1.0");
            when(versionResolver.generateMediaType("1.0", "json"))
                    .thenReturn("application/vnd.apigen.v1.0+json");
            when(versionResolver.generateMediaType("2.0", "json"))
                    .thenReturn("application/vnd.apigen.v2.0+json");
            when(versionResolver.generateMediaType("3.0", "json"))
                    .thenReturn("application/vnd.apigen.v3.0+json");

            ResponseEntity<ApiVersionController.ApiVersionInfo> response =
                    controller.getVersionInfo();

            List<ApiVersionController.VersionDetail> versions =
                    response.getBody().supportedVersions();
            assertThat(versions)
                    .extracting(ApiVersionController.VersionDetail::mediaType)
                    .containsExactly(
                            "application/vnd.apigen.v1.0+json",
                            "application/vnd.apigen.v2.0+json",
                            "application/vnd.apigen.v3.0+json");
        }
    }
}
