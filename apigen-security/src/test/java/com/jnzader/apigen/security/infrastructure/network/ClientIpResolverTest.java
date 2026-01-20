package com.jnzader.apigen.security.infrastructure.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.TrustedProxiesProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.TrustedProxiesProperties.TrustMode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ClientIpResolver")
class ClientIpResolverTest {

    private SecurityProperties securityProperties;
    private TrustedProxiesProperties trustedProxiesProperties;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        securityProperties = mock(SecurityProperties.class);
        trustedProxiesProperties = new TrustedProxiesProperties();
        when(securityProperties.getTrustedProxies()).thenReturn(trustedProxiesProperties);
        request = mock(HttpServletRequest.class);
    }

    @Nested
    @DisplayName("TRUST_DIRECT mode")
    class TrustDirectMode {

        @BeforeEach
        void setUp() {
            trustedProxiesProperties.setMode(TrustMode.TRUST_DIRECT);
        }

        @Test
        @DisplayName("should always use remoteAddr, ignoring X-Forwarded-For")
        void shouldIgnoreForwardedHeaders() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("should return remoteAddr when no headers present")
        void shouldReturnRemoteAddrWhenNoHeaders() {
            when(request.getRemoteAddr()).thenReturn("203.0.113.50");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("203.0.113.50");
        }
    }

    @Nested
    @DisplayName("TRUST_ALL mode")
    class TrustAllMode {

        @BeforeEach
        void setUp() {
            trustedProxiesProperties.setMode(TrustMode.TRUST_ALL);
            trustedProxiesProperties.setForwardedForHeader("X-Forwarded-For");
            trustedProxiesProperties.setUseFirstInChain(true);
        }

        @Test
        @DisplayName("should use first IP from X-Forwarded-For header")
        void shouldUseFirstIpFromXForwardedFor() {
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Forwarded-For"))
                    .thenReturn("203.0.113.50, 10.0.0.2, 10.0.0.1");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("should use last IP from X-Forwarded-For when useFirstInChain is false")
        void shouldUseLastIpWhenConfigured() {
            trustedProxiesProperties.setUseFirstInChain(false);
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Forwarded-For"))
                    .thenReturn("203.0.113.50, 10.0.0.2, 10.0.0.3");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("10.0.0.3");
        }

        @Test
        @DisplayName("should fallback to X-Real-IP if X-Forwarded-For is missing")
        void shouldFallbackToXRealIp() {
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.60");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("203.0.113.60");
        }

        @Test
        @DisplayName("should ignore 'unknown' header values")
        void shouldIgnoreUnknownHeaderValues() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should return remoteAddr if all headers are blank")
        void shouldReturnRemoteAddrIfHeadersBlank() {
            when(request.getRemoteAddr()).thenReturn("172.16.0.50");
            when(request.getHeader("X-Forwarded-For")).thenReturn("  ");
            when(request.getHeader("X-Real-IP")).thenReturn("");
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("172.16.0.50");
        }
    }

    @Nested
    @DisplayName("CONFIGURED mode")
    class ConfiguredMode {

        @BeforeEach
        void setUp() {
            trustedProxiesProperties.setMode(TrustMode.CONFIGURED);
            trustedProxiesProperties.setForwardedForHeader("X-Forwarded-For");
            trustedProxiesProperties.setUseFirstInChain(true);
            trustedProxiesProperties.setAddresses(List.of("10.0.0.1", "10.0.0.2"));
        }

        @Test
        @DisplayName("should use X-Forwarded-For when connection is from trusted proxy")
        void shouldUseHeaderWhenFromTrustedProxy() {
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("should ignore X-Forwarded-For when connection is NOT from trusted proxy")
        void shouldIgnoreHeaderWhenNotFromTrustedProxy() {
            when(request.getRemoteAddr()).thenReturn("203.0.113.99");
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.10.10.10");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("203.0.113.99");
        }

        @Test
        @DisplayName("should return remoteAddr if header is missing from trusted proxy")
        void shouldReturnRemoteAddrIfHeaderMissing() {
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName(
                "should handle IP chain and return first non-trusted IP when useFirstInChain is"
                        + " false")
        void shouldReturnFirstNonTrustedInChain() {
            trustedProxiesProperties.setUseFirstInChain(false);
            trustedProxiesProperties.setAddresses(List.of("10.0.0.1", "10.0.0.2", "10.0.0.3"));

            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Forwarded-For"))
                    .thenReturn("203.0.113.50, 10.0.0.3, 10.0.0.2");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("203.0.113.50");
        }
    }

    @Nested
    @DisplayName("CIDR range matching")
    class CidrRangeMatching {

        @BeforeEach
        void setUp() {
            trustedProxiesProperties.setMode(TrustMode.CONFIGURED);
            trustedProxiesProperties.setForwardedForHeader("X-Forwarded-For");
            trustedProxiesProperties.setUseFirstInChain(true);
        }

        @Test
        @DisplayName("should match IP in /24 CIDR range")
        void shouldMatchIpInCidr24Range() {
            trustedProxiesProperties.setAddresses(List.of("10.0.0.0/24"));
            when(request.getRemoteAddr()).thenReturn("10.0.0.50");
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("should not match IP outside /24 CIDR range")
        void shouldNotMatchIpOutsideCidr24Range() {
            trustedProxiesProperties.setAddresses(List.of("10.0.0.0/24"));
            when(request.getRemoteAddr()).thenReturn("10.0.1.50");
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("10.0.1.50");
        }

        @Test
        @DisplayName("should match IP in /8 private CIDR range")
        void shouldMatchIpInCidr8Range() {
            trustedProxiesProperties.setAddresses(List.of("10.0.0.0/8"));
            when(request.getRemoteAddr()).thenReturn("10.255.255.255");
            when(request.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("8.8.8.8");
        }

        @Test
        @DisplayName("should match IP in /16 range")
        void shouldMatchIpInCidr16Range() {
            trustedProxiesProperties.setAddresses(List.of("172.16.0.0/12"));
            when(request.getRemoteAddr()).thenReturn("172.31.255.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("1.2.3.4");
        }

        @Test
        @DisplayName("should match exact IP when no CIDR specified")
        void shouldMatchExactIpWithoutCidr() {
            trustedProxiesProperties.setAddresses(List.of("192.168.1.100"));
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");
            when(request.getHeader("X-Forwarded-For")).thenReturn("5.6.7.8");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("5.6.7.8");
        }

        @Test
        @DisplayName("should not match different exact IP")
        void shouldNotMatchDifferentExactIp() {
            trustedProxiesProperties.setAddresses(List.of("192.168.1.100"));
            when(request.getRemoteAddr()).thenReturn("192.168.1.101");
            when(request.getHeader("X-Forwarded-For")).thenReturn("5.6.7.8");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("192.168.1.101");
        }
    }

    @Nested
    @DisplayName("IPv6 support")
    class IPv6Support {

        @BeforeEach
        void setUp() {
            trustedProxiesProperties.setMode(TrustMode.CONFIGURED);
            trustedProxiesProperties.setForwardedForHeader("X-Forwarded-For");
            trustedProxiesProperties.setUseFirstInChain(true);
        }

        @Test
        @DisplayName("should match IPv6 loopback address")
        void shouldMatchIPv6Loopback() {
            trustedProxiesProperties.setAddresses(List.of("::1"));
            when(request.getRemoteAddr()).thenReturn("::1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("2001:db8::1");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("2001:db8::1");
        }

        @Test
        @DisplayName("should match IPv6 CIDR range")
        void shouldMatchIPv6CidrRange() {
            trustedProxiesProperties.setAddresses(List.of("2001:db8::/32"));
            when(request.getRemoteAddr()).thenReturn("2001:db8:1234::5678");
            when(request.getHeader("X-Forwarded-For")).thenReturn("2607:f8b0:4005::1");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("2607:f8b0:4005::1");
        }
    }

    @Nested
    @DisplayName("isTrustedProxy method")
    class IsTrustedProxyMethod {

        @BeforeEach
        void setUp() {
            trustedProxiesProperties.setMode(TrustMode.CONFIGURED);
            trustedProxiesProperties.setAddresses(List.of("10.0.0.0/8", "192.168.1.1"));
        }

        @Test
        @DisplayName("should return true for IP in trusted range")
        void shouldReturnTrueForTrustedIp() {
            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            assertThat(resolver.isTrustedProxy("10.20.30.40")).isTrue();
        }

        @Test
        @DisplayName("should return true for exact trusted IP")
        void shouldReturnTrueForExactTrustedIp() {
            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            assertThat(resolver.isTrustedProxy("192.168.1.1")).isTrue();
        }

        @Test
        @DisplayName("should return false for untrusted IP")
        void shouldReturnFalseForUntrustedIp() {
            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            assertThat(resolver.isTrustedProxy("203.0.113.50")).isFalse();
        }

        @Test
        @DisplayName("should return false for null IP")
        void shouldReturnFalseForNullIp() {
            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            assertThat(resolver.isTrustedProxy(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for blank IP")
        void shouldReturnFalseForBlankIp() {
            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            assertThat(resolver.isTrustedProxy("  ")).isFalse();
        }

        @Test
        @DisplayName("should return false for invalid IP format")
        void shouldReturnFalseForInvalidIp() {
            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            assertThat(resolver.isTrustedProxy("not.an.ip.address")).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty trusted addresses list")
        void shouldHandleEmptyTrustedAddresses() {
            trustedProxiesProperties.setMode(TrustMode.CONFIGURED);
            trustedProxiesProperties.setAddresses(List.of());
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            // Should ignore header since no trusted proxies configured
            assertThat(clientIp).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("should skip invalid CIDR addresses gracefully")
        void shouldSkipInvalidCidrAddresses() {
            trustedProxiesProperties.setMode(TrustMode.CONFIGURED);
            trustedProxiesProperties.setAddresses(
                    List.of("invalid-ip", "10.0.0.1", "also.invalid"));
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            // Should still work with valid address
            assertThat(clientIp).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("should trim whitespace from IPs in X-Forwarded-For")
        void shouldTrimWhitespaceFromIps() {
            trustedProxiesProperties.setMode(TrustMode.TRUST_ALL);
            trustedProxiesProperties.setUseFirstInChain(true);
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.50  , 10.0.0.2 ");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("should use custom header name when configured")
        void shouldUseCustomHeaderName() {
            trustedProxiesProperties.setMode(TrustMode.TRUST_ALL);
            trustedProxiesProperties.setForwardedForHeader("CF-Connecting-IP");
            trustedProxiesProperties.setUseFirstInChain(true);
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.77");

            ClientIpResolver resolver = new ClientIpResolver(securityProperties);
            resolver.initialize();

            String clientIp = resolver.resolveClientIp(request);

            assertThat(clientIp).isEqualTo("203.0.113.77");
        }
    }
}
