package com.jnzader.apigen.core.infrastructure.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TenantFilter Tests")
class TenantFilterTest {

    private TenantResolver resolver;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        resolver = TenantResolver.builder().strategies(TenantResolutionStrategy.HEADER).build();

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Nested
    @DisplayName("Tenant Resolution")
    class TenantResolutionTests {

        @Test
        @DisplayName("should set tenant context from header")
        void shouldSetTenantContextFromHeader() throws Exception {
            when(request.getHeader("X-Tenant-ID")).thenReturn("acme-corp");
            when(request.getRequestURI()).thenReturn("/api/products");

            TenantFilter filter = new TenantFilter(resolver, false, Set.of());

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verify(response).setHeader("X-Tenant-ID", "acme-corp");
        }

        @Test
        @DisplayName("should clear tenant context after request")
        void shouldClearTenantContextAfterRequest() throws Exception {
            when(request.getHeader("X-Tenant-ID")).thenReturn("acme-corp");
            when(request.getRequestURI()).thenReturn("/api/products");

            TenantFilter filter = new TenantFilter(resolver, false, Set.of());

            filter.doFilter(request, response, chain);

            // Context should be cleared after filter completes
            assertThat(TenantContext.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("Required Tenant")
    class RequiredTenantTests {

        @Test
        @DisplayName("should return 400 when tenant required but not found")
        void shouldReturn400WhenTenantRequiredButNotFound() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/products");

            TenantFilter filter = new TenantFilter(resolver, true, Set.of());

            filter.doFilter(request, response, chain);

            verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
            verify(chain, never()).doFilter(request, response);
            assertThat(responseWriter.toString()).contains("Tenant Required");
        }

        @Test
        @DisplayName("should continue when tenant not required and not found")
        void shouldContinueWhenTenantNotRequiredAndNotFound() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/products");

            TenantFilter filter = new TenantFilter(resolver, false, Set.of());

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Invalid Tenant")
    class InvalidTenantTests {

        @Test
        @DisplayName("should return 400 for invalid tenant ID")
        void shouldReturn400ForInvalidTenantId() throws Exception {
            when(request.getHeader("X-Tenant-ID")).thenReturn("-invalid-");
            when(request.getRequestURI()).thenReturn("/api/products");

            TenantFilter filter = new TenantFilter(resolver, false, Set.of());

            filter.doFilter(request, response, chain);

            verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
            verify(chain, never()).doFilter(request, response);
            assertThat(responseWriter.toString()).contains("Invalid Tenant");
        }
    }

    @Nested
    @DisplayName("Excluded Paths")
    class ExcludedPathsTests {

        @Test
        @DisplayName("should skip tenant resolution for excluded paths")
        void shouldSkipTenantResolutionForExcludedPaths() throws Exception {
            when(request.getRequestURI()).thenReturn("/actuator/health");

            TenantFilter filter = new TenantFilter(resolver, true, Set.of("/actuator/**"));

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verify(response, never()).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("should skip tenant resolution for exact path match")
        void shouldSkipTenantResolutionForExactPathMatch() throws Exception {
            when(request.getRequestURI()).thenReturn("/health");

            TenantFilter filter = new TenantFilter(resolver, true, Set.of("/health"));

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("should require tenant for non-excluded paths")
        void shouldRequireTenantForNonExcludedPaths() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/products");

            TenantFilter filter = new TenantFilter(resolver, true, Set.of("/actuator/**"));

            filter.doFilter(request, response, chain);

            verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
