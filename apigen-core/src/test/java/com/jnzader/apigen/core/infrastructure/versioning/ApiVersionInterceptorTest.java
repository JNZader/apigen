package com.jnzader.apigen.core.infrastructure.versioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

@DisplayName("ApiVersionInterceptor Tests")
class ApiVersionInterceptorTest {

    private ApiVersionResolver resolver;
    private ApiVersionInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        resolver =
                ApiVersionResolver.builder()
                        .strategies(VersioningStrategy.HEADER)
                        .defaultVersion("1.0")
                        .build();
        interceptor = new ApiVersionInterceptor(resolver);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Nested
    @DisplayName("Version Header")
    class VersionHeaderTests {

        @Test
        @DisplayName("should add X-API-Version header to response")
        void shouldAddVersionHeaderToResponse() {
            when(request.getHeader("X-API-Version")).thenReturn("2.0");

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            verify(response).setHeader("X-API-Version", "2.0");
        }

        @Test
        @DisplayName("should use default version when none provided")
        void shouldUseDefaultVersionWhenNoneProvided() {
            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            verify(response).setHeader("X-API-Version", "1.0");
        }

        @Test
        @DisplayName("should store version in request attribute")
        void shouldStoreVersionInRequestAttribute() {
            when(request.getHeader("X-API-Version")).thenReturn("3.0");

            interceptor.preHandle(request, response, new Object());

            verify(request).setAttribute("apigen.api.version", "3.0");
        }
    }

    @Nested
    @DisplayName("Deprecation Headers")
    class DeprecationHeadersTests {

        @Test
        @DisplayName("should add Deprecation header for deprecated endpoint")
        void shouldAddDeprecationHeader() throws Exception {
            when(request.getHeader("X-API-Version")).thenReturn("1.0");

            HandlerMethod handlerMethod = createHandlerMethod(DeprecatedController.class);

            interceptor.preHandle(request, response, handlerMethod);

            verify(response).setHeader("Deprecation", "Mon, 1 Jan 2024 00:00:00 GMT");
        }

        @Test
        @DisplayName("should add Sunset header when sunset date is specified")
        void shouldAddSunsetHeader() throws Exception {
            when(request.getHeader("X-API-Version")).thenReturn("1.0");

            HandlerMethod handlerMethod = createHandlerMethod(DeprecatedWithSunsetController.class);

            interceptor.preHandle(request, response, handlerMethod);

            verify(response).setHeader("Sunset", "Wed, 1 Jan 2025 00:00:00 GMT");
        }

        @Test
        @DisplayName("should add Link header for successor version")
        void shouldAddLinkHeaderForSuccessor() throws Exception {
            when(request.getHeader("X-API-Version")).thenReturn("1.0");
            when(request.getRequestURI()).thenReturn("/api/v1/products");

            HandlerMethod handlerMethod = createHandlerMethod(DeprecatedWithSuccessorController.class);

            interceptor.preHandle(request, response, handlerMethod);

            verify(response).setHeader("Link", "</api/v2.0/products>; rel=\"successor-version\"");
        }

        @Test
        @DisplayName("should add Link header for migration guide")
        void shouldAddLinkHeaderForMigrationGuide() throws Exception {
            when(request.getHeader("X-API-Version")).thenReturn("1.0");
            when(request.getRequestURI()).thenReturn("/api/v1/products");

            HandlerMethod handlerMethod = createHandlerMethod(DeprecatedWithMigrationController.class);

            interceptor.preHandle(request, response, handlerMethod);

            verify(response)
                    .setHeader(
                            "Link",
                            "</api/v2.0/products>; rel=\"successor-version\", "
                                    + "<https://docs.example.com/migration>; "
                                    + "rel=\"deprecation\"; type=\"text/html\"");
        }

        private HandlerMethod createHandlerMethod(Class<?> controllerClass) throws Exception {
            Method method = controllerClass.getMethod("handle");
            Object controller = controllerClass.getDeclaredConstructor().newInstance();
            return new HandlerMethod(controller, method);
        }

        // Test controller classes
        @DeprecatedVersion(since = "2024-01-01")
        static class DeprecatedController {
            public void handle() {}
        }

        @DeprecatedVersion(since = "2024-01-01", sunset = "2025-01-01")
        static class DeprecatedWithSunsetController {
            public void handle() {}
        }

        @DeprecatedVersion(since = "2024-01-01", successor = "2.0")
        static class DeprecatedWithSuccessorController {
            public void handle() {}
        }

        @DeprecatedVersion(
                since = "2024-01-01",
                successor = "2.0",
                migrationGuide = "https://docs.example.com/migration")
        static class DeprecatedWithMigrationController {
            public void handle() {}
        }
    }

    @Nested
    @DisplayName("Static Methods")
    class StaticMethodTests {

        @Test
        @DisplayName("should get version from request attribute")
        void shouldGetVersionFromRequestAttribute() {
            when(request.getAttribute("apigen.api.version")).thenReturn("2.5");

            String version = ApiVersionInterceptor.getVersion(request);

            assertThat(version).isEqualTo("2.5");
        }

        @Test
        @DisplayName("should return null when version not set")
        void shouldReturnNullWhenVersionNotSet() {
            when(request.getAttribute("apigen.api.version")).thenReturn(null);

            String version = ApiVersionInterceptor.getVersion(request);

            assertThat(version).isNull();
        }
    }
}
