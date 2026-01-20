package com.jnzader.apigen.core.infrastructure.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TenantResolver Tests")
class TenantResolverTest {

    @Nested
    @DisplayName("Header Resolution")
    class HeaderResolutionTests {

        @Test
        @DisplayName("should resolve tenant from X-Tenant-ID header")
        void shouldResolveFromXTenantIdHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-ID")).thenReturn("acme-corp");

            TenantResolver resolver =
                    TenantResolver.builder().strategies(TenantResolutionStrategy.HEADER).build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("acme-corp");
        }

        @Test
        @DisplayName("should use custom header name")
        void shouldUseCustomHeaderName() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("My-Tenant")).thenReturn("custom-tenant");

            TenantResolver resolver =
                    TenantResolver.builder()
                            .strategies(TenantResolutionStrategy.HEADER)
                            .tenantHeader("My-Tenant")
                            .build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("custom-tenant");
        }

        @Test
        @DisplayName("should return default when no header present")
        void shouldReturnDefaultWhenNoHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);

            TenantResolver resolver =
                    TenantResolver.builder()
                            .strategies(TenantResolutionStrategy.HEADER)
                            .defaultTenant("default")
                            .build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("default");
        }

        @Test
        @DisplayName("should return null when no header and no default")
        void shouldReturnNullWhenNoHeaderAndNoDefault() {
            HttpServletRequest request = mock(HttpServletRequest.class);

            TenantResolver resolver =
                    TenantResolver.builder().strategies(TenantResolutionStrategy.HEADER).build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isNull();
        }
    }

    @Nested
    @DisplayName("Subdomain Resolution")
    class SubdomainResolutionTests {

        @Test
        @DisplayName("should resolve tenant from subdomain")
        void shouldResolveFromSubdomain() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getServerName()).thenReturn("acme.example.com");

            TenantResolver resolver =
                    TenantResolver.builder().strategies(TenantResolutionStrategy.SUBDOMAIN).build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("acme");
        }

        @Test
        @DisplayName("should not resolve www as tenant")
        void shouldNotResolveWwwAsTenant() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getServerName()).thenReturn("www.example.com");

            TenantResolver resolver =
                    TenantResolver.builder()
                            .strategies(TenantResolutionStrategy.SUBDOMAIN)
                            .defaultTenant("default")
                            .build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("default");
        }

        @Test
        @DisplayName("should not resolve api as tenant")
        void shouldNotResolveApiAsTenant() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getServerName()).thenReturn("api.example.com");

            TenantResolver resolver =
                    TenantResolver.builder()
                            .strategies(TenantResolutionStrategy.SUBDOMAIN)
                            .defaultTenant("default")
                            .build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("Path Resolution")
    class PathResolutionTests {

        @Test
        @DisplayName("should resolve tenant from path")
        void shouldResolveFromPath() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/tenants/acme-corp/products");

            TenantResolver resolver =
                    TenantResolver.builder().strategies(TenantResolutionStrategy.PATH).build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("acme-corp");
        }

        @Test
        @DisplayName("should use custom path prefix")
        void shouldUseCustomPathPrefix() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/org/my-org/users");

            TenantResolver resolver =
                    TenantResolver.builder()
                            .strategies(TenantResolutionStrategy.PATH)
                            .pathPrefix("org")
                            .build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("my-org");
        }

        @Test
        @DisplayName("should return default when no tenant in path")
        void shouldReturnDefaultWhenNoTenantInPath() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURI()).thenReturn("/api/products");

            TenantResolver resolver =
                    TenantResolver.builder()
                            .strategies(TenantResolutionStrategy.PATH)
                            .defaultTenant("default")
                            .build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("JWT Claim Resolution")
    class JwtClaimResolutionTests {

        @Test
        @DisplayName("should resolve tenant from request attribute")
        void shouldResolveFromRequestAttribute() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute("tenant.id")).thenReturn("jwt-tenant");

            TenantResolver resolver =
                    TenantResolver.builder().strategies(TenantResolutionStrategy.JWT_CLAIM).build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("jwt-tenant");
        }
    }

    @Nested
    @DisplayName("Multiple Strategies")
    class MultipleStrategiesTests {

        @Test
        @DisplayName("should try strategies in order")
        void shouldTryStrategiesInOrder() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-ID")).thenReturn(null);
            when(request.getServerName()).thenReturn("tenant2.example.com");

            TenantResolver resolver =
                    TenantResolver.builder()
                            .strategies(
                                    TenantResolutionStrategy.HEADER,
                                    TenantResolutionStrategy.SUBDOMAIN)
                            .build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("tenant2");
        }

        @Test
        @DisplayName("should use first matching strategy")
        void shouldUseFirstMatchingStrategy() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-ID")).thenReturn("header-tenant");
            when(request.getServerName()).thenReturn("subdomain-tenant.example.com");

            TenantResolver resolver =
                    TenantResolver.builder()
                            .strategies(
                                    TenantResolutionStrategy.HEADER,
                                    TenantResolutionStrategy.SUBDOMAIN)
                            .build();

            String tenant = resolver.resolve(request);

            assertThat(tenant).isEqualTo("header-tenant");
        }
    }

    @Nested
    @DisplayName("Tenant Validation")
    class TenantValidationTests {

        @Test
        @DisplayName("should validate correct tenant IDs")
        void shouldValidateCorrectTenantIds() {
            TenantResolver resolver = TenantResolver.builder().build();

            assertThat(resolver.isValidTenantId("acme-corp")).isTrue();
            assertThat(resolver.isValidTenantId("tenant123")).isTrue();
            assertThat(resolver.isValidTenantId("my-company-name")).isTrue();
        }

        @Test
        @DisplayName("should reject invalid tenant IDs")
        void shouldRejectInvalidTenantIds() {
            TenantResolver resolver = TenantResolver.builder().build();

            assertThat(resolver.isValidTenantId(null)).isFalse();
            assertThat(resolver.isValidTenantId("")).isFalse();
            assertThat(resolver.isValidTenantId("  ")).isFalse();
            assertThat(resolver.isValidTenantId("a")).isFalse(); // Too short
            assertThat(resolver.isValidTenantId("-invalid")).isFalse(); // Starts with hyphen
            assertThat(resolver.isValidTenantId("invalid-")).isFalse(); // Ends with hyphen
        }
    }

    @Nested
    @DisplayName("Resolve By Strategy")
    class ResolveByStrategyTests {

        @Test
        @DisplayName("should resolve by specific strategy")
        void shouldResolveBySpecificStrategy() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-ID")).thenReturn("header-tenant");
            when(request.getServerName()).thenReturn("subdomain-tenant.example.com");

            TenantResolver resolver = TenantResolver.builder().build();

            Optional<String> headerTenant =
                    resolver.resolveByStrategy(request, TenantResolutionStrategy.HEADER);
            Optional<String> subdomainTenant =
                    resolver.resolveByStrategy(request, TenantResolutionStrategy.SUBDOMAIN);

            assertThat(headerTenant).contains("header-tenant");
            assertThat(subdomainTenant).contains("subdomain-tenant");
        }
    }
}
