package com.jnzader.apigen.core.infrastructure.versioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ApiVersionResolver Tests")
class ApiVersionResolverTest {

    @Nested
    @DisplayName("Header Resolution")
    class HeaderResolutionTests {

        @Test
        @DisplayName("should resolve version from X-API-Version header")
        void shouldResolveFromXApiVersionHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-API-Version")).thenReturn("2.0");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.HEADER)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("2.0");
        }

        @Test
        @DisplayName("should resolve version from Accept-Version header")
        void shouldResolveFromAcceptVersionHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-API-Version")).thenReturn(null);
            when(request.getHeader("Accept-Version")).thenReturn("3.0");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.HEADER)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("3.0");
        }

        @Test
        @DisplayName("should resolve version from API-Version header")
        void shouldResolveFromApiVersionHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-API-Version")).thenReturn(null);
            when(request.getHeader("Accept-Version")).thenReturn(null);
            when(request.getHeader("API-Version")).thenReturn("1.5");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.HEADER)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("1.5");
        }

        @Test
        @DisplayName("should use custom header name")
        void shouldUseCustomHeaderName() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("My-Version")).thenReturn("4.0");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.HEADER)
                            .versionHeader("My-Version")
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("4.0");
        }

        @Test
        @DisplayName("should return default when no header present")
        void shouldReturnDefaultWhenNoHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.HEADER)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("1.0");
        }
    }

    @Nested
    @DisplayName("Path Resolution")
    class PathResolutionTests {

        @Test
        @DisplayName("should resolve version from path")
        void shouldResolveFromPath() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/v2/products");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.PATH)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("2");
        }

        @Test
        @DisplayName("should resolve version with minor number from path")
        void shouldResolveVersionWithMinorFromPath() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/v2.1/products");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.PATH)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("2.1");
        }

        @Test
        @DisplayName("should use custom path prefix")
        void shouldUseCustomPathPrefix() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/version3/products");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.PATH)
                            .pathPrefix("version")
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("3");
        }

        @Test
        @DisplayName("should return default when no version in path")
        void shouldReturnDefaultWhenNoVersionInPath() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/products");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.PATH)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("1.0");
        }
    }

    @Nested
    @DisplayName("Query Parameter Resolution")
    class QueryParamResolutionTests {

        @Test
        @DisplayName("should resolve version from query parameter")
        void shouldResolveFromQueryParam() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getParameter("version")).thenReturn("2.0");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.QUERY_PARAM)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("2.0");
        }

        @Test
        @DisplayName("should use custom query parameter name")
        void shouldUseCustomQueryParamName() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getParameter("api_version")).thenReturn("3.0");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.QUERY_PARAM)
                            .versionParam("api_version")
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("3.0");
        }
    }

    @Nested
    @DisplayName("Media Type Resolution")
    class MediaTypeResolutionTests {

        @Test
        @DisplayName("should resolve version from Accept media type")
        void shouldResolveFromMediaType() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Accept")).thenReturn("application/vnd.api.v2+json");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.MEDIA_TYPE)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("2");
        }

        @Test
        @DisplayName("should resolve version with minor from media type")
        void shouldResolveVersionWithMinorFromMediaType() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Accept")).thenReturn("application/vnd.myapi.v3.1+json");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.MEDIA_TYPE)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("3.1");
        }
    }

    @Nested
    @DisplayName("Multiple Strategies")
    class MultipleStrategiesTests {

        @Test
        @DisplayName("should try strategies in order")
        void shouldTryStrategiesInOrder() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-API-Version")).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/api/v3/products");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.HEADER, VersioningStrategy.PATH)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("3");
        }

        @Test
        @DisplayName("should use first matching strategy")
        void shouldUseFirstMatchingStrategy() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-API-Version")).thenReturn("2.0");
            when(request.getRequestURI()).thenReturn("/api/v3/products");

            ApiVersionResolver resolver =
                    ApiVersionResolver.builder()
                            .strategies(VersioningStrategy.HEADER, VersioningStrategy.PATH)
                            .defaultVersion("1.0")
                            .build();

            String version = resolver.resolve(request);

            assertThat(version).isEqualTo("2.0");
        }
    }

    @Nested
    @DisplayName("Version Matching")
    class VersionMatchingTests {

        @Test
        @DisplayName("should match exact version")
        void shouldMatchExactVersion() {
            ApiVersionResolver resolver = ApiVersionResolver.builder().build();

            assertThat(resolver.matches("2.0", "2.0")).isTrue();
        }

        @Test
        @DisplayName("should match major version")
        void shouldMatchMajorVersion() {
            ApiVersionResolver resolver = ApiVersionResolver.builder().build();

            assertThat(resolver.matches("2", "2.0")).isTrue();
            assertThat(resolver.matches("2.0", "2")).isTrue();
        }

        @Test
        @DisplayName("should not match different versions")
        void shouldNotMatchDifferentVersions() {
            ApiVersionResolver resolver = ApiVersionResolver.builder().build();

            assertThat(resolver.matches("1.0", "2.0")).isFalse();
        }

        @Test
        @DisplayName("should handle null versions")
        void shouldHandleNullVersions() {
            ApiVersionResolver resolver = ApiVersionResolver.builder().build();

            assertThat(resolver.matches(null, "1.0")).isFalse();
            assertThat(resolver.matches("1.0", null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Resolve By Strategy")
    class ResolveByStrategyTests {

        @Test
        @DisplayName("should resolve by specific strategy")
        void shouldResolveBySpecificStrategy() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-API-Version")).thenReturn("2.0");
            when(request.getRequestURI()).thenReturn("/api/v3/products");

            ApiVersionResolver resolver = ApiVersionResolver.builder().build();

            Optional<String> headerVersion =
                    resolver.resolveByStrategy(request, VersioningStrategy.HEADER);
            Optional<String> pathVersion =
                    resolver.resolveByStrategy(request, VersioningStrategy.PATH);

            assertThat(headerVersion).contains("2.0");
            assertThat(pathVersion).contains("3");
        }
    }
}
