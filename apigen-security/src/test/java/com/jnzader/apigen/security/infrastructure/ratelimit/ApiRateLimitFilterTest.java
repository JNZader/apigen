package com.jnzader.apigen.security.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties.StorageMode;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties.TierConfig;
import com.jnzader.apigen.security.infrastructure.network.ClientIpResolver;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiRateLimitFilter")
class ApiRateLimitFilterTest {

    @Mock private RateLimitService rateLimitService;
    @Mock private ClientIpResolver clientIpResolver;
    @Mock private FilterChain filterChain;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private SecurityProperties securityProperties;
    private ApiRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        RateLimitProperties rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setEnabled(true);
        rateLimitProperties.setStorageMode(StorageMode.IN_MEMORY);
        rateLimitProperties.setRequestsPerSecond(100);
        rateLimitProperties.setBurstCapacity(150);
        rateLimitProperties.setAuthRequestsPerMinute(10);
        rateLimitProperties.setAuthBurstCapacity(15);
        securityProperties.setRateLimit(rateLimitProperties);

        lenient().when(rateLimitService.isTiersEnabled()).thenReturn(false);

        filter =
                new ApiRateLimitFilter(
                        rateLimitService, securityProperties, clientIpResolver, null, objectMapper);
    }

    private MockHttpServletRequest createRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        return request;
    }

    private ConsumptionProbe createConsumedProbe(long remaining) {
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        lenient().when(probe.isConsumed()).thenReturn(true);
        lenient().when(probe.getRemainingTokens()).thenReturn(remaining);
        lenient().when(probe.getNanosToWaitForRefill()).thenReturn(0L);
        return probe;
    }

    private ConsumptionProbe createRejectedProbe(long waitNanos) {
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        lenient().when(probe.isConsumed()).thenReturn(false);
        lenient().when(probe.getRemainingTokens()).thenReturn(0L);
        lenient().when(probe.getNanosToWaitForRefill()).thenReturn(waitNanos);
        return probe;
    }

    @Nested
    @DisplayName("Skip rate limiting")
    class SkipRateLimitingTests {

        @ParameterizedTest(name = "should skip rate limiting for {0}")
        @ValueSource(
                strings = {
                    "/actuator/health",
                    "/actuator/health/liveness",
                    "/actuator/info",
                    "/swagger-ui/index.html",
                    "/swagger-ui.html",
                    "/v3/api-docs",
                    "/v3/api-docs/swagger-config",
                    "/favicon.ico",
                    "/static/bundle.js",
                    "/assets/styles.css",
                    "/images/logo.png",
                    "/images/banner.jpg",
                    "/icons/favicon.ico"
                })
        void shouldSkipRateLimitingForStaticResources(String uri) throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("GET", uri);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(rateLimitService, never())
                    .tryConsumeAndReturnRemaining(anyString(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("IP-based rate limiting")
    class IpBasedRateLimitingTests {

        @Test
        @DisplayName("should allow request when within limit")
        void shouldAllowRequestWhenWithinLimit() throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("GET", "/api/users");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String clientIp = "192.168.1.1";

            // Create probe first, then setup stubbing
            ConsumptionProbe probe = createConsumedProbe(149);

            when(clientIpResolver.resolveClientIp(request)).thenReturn(clientIp);
            when(rateLimitService.tryConsumeAndReturnRemaining(clientIp, false)).thenReturn(probe);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("150");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("149");
        }

        @Test
        @DisplayName("should block request when limit exceeded")
        void shouldBlockRequestWhenLimitExceeded() throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("GET", "/api/users");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String clientIp = "192.168.1.2";
            long waitNanos = 5_000_000_000L; // 5 seconds

            // Create probe first
            ConsumptionProbe probe = createRejectedProbe(waitNanos);

            when(clientIpResolver.resolveClientIp(request)).thenReturn(clientIp);
            when(rateLimitService.tryConsumeAndReturnRemaining(clientIp, false)).thenReturn(probe);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, never()).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getContentType()).isEqualTo("application/problem+json");
            assertThat(response.getHeader("Retry-After")).isEqualTo("5");
            assertThat(response.getContentAsString()).contains("Too Many Requests");
        }

        @Test
        @DisplayName("should use auth endpoint limits for POST /auth/login")
        void shouldUseAuthEndpointLimitsForLogin() throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("POST", "/api/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String clientIp = "192.168.1.3";

            ConsumptionProbe probe = createConsumedProbe(14);

            when(clientIpResolver.resolveClientIp(request)).thenReturn(clientIp);
            when(rateLimitService.tryConsumeAndReturnRemaining(clientIp, true)).thenReturn(probe);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsumeAndReturnRemaining(clientIp, true);
            assertThat(response.getHeader("X-RateLimit-Limit"))
                    .isEqualTo("15"); // authBurstCapacity
        }

        @Test
        @DisplayName("should use auth endpoint limits for POST /auth/register")
        void shouldUseAuthEndpointLimitsForRegister() throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("POST", "/api/auth/register");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String clientIp = "192.168.1.4";

            ConsumptionProbe probe = createConsumedProbe(14);

            when(clientIpResolver.resolveClientIp(request)).thenReturn(clientIp);
            when(rateLimitService.tryConsumeAndReturnRemaining(clientIp, true)).thenReturn(probe);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsumeAndReturnRemaining(clientIp, true);
        }

        @Test
        @DisplayName("should use auth endpoint limits for POST /auth/refresh")
        void shouldUseAuthEndpointLimitsForRefresh() throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("POST", "/api/auth/refresh");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String clientIp = "192.168.1.5";

            ConsumptionProbe probe = createConsumedProbe(14);

            when(clientIpResolver.resolveClientIp(request)).thenReturn(clientIp);
            when(rateLimitService.tryConsumeAndReturnRemaining(clientIp, true)).thenReturn(probe);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsumeAndReturnRemaining(clientIp, true);
        }

        @Test
        @DisplayName("should NOT use auth limits for GET /auth/login")
        void shouldNotUseAuthLimitsForGetLogin() throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("GET", "/api/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String clientIp = "192.168.1.6";

            ConsumptionProbe probe = createConsumedProbe(149);

            when(clientIpResolver.resolveClientIp(request)).thenReturn(clientIp);
            when(rateLimitService.tryConsumeAndReturnRemaining(clientIp, false)).thenReturn(probe);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(rateLimitService).tryConsumeAndReturnRemaining(clientIp, false);
        }

        @Test
        @DisplayName("should include error message for auth endpoint rate limit")
        void shouldIncludeErrorMessageForAuthEndpoint() throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("POST", "/api/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String clientIp = "192.168.1.7";
            long waitNanos = 10_000_000_000L; // 10 seconds

            ConsumptionProbe probe = createRejectedProbe(waitNanos);

            when(clientIpResolver.resolveClientIp(request)).thenReturn(clientIp);
            when(rateLimitService.tryConsumeAndReturnRemaining(clientIp, true)).thenReturn(probe);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getContentAsString()).contains("authentication");
            assertThat(response.getContentAsString()).contains("/auth");
        }
    }

    @Nested
    @DisplayName("Tier-based rate limiting")
    class TierBasedRateLimitingTests {

        @Mock private RateLimitTierResolver tierResolver;

        private ApiRateLimitFilter tierFilter;

        @BeforeEach
        void setUpTierFilter() {
            lenient().when(rateLimitService.isTiersEnabled()).thenReturn(true);
            tierFilter =
                    new ApiRateLimitFilter(
                            rateLimitService,
                            securityProperties,
                            clientIpResolver,
                            tierResolver,
                            objectMapper);
        }

        @Test
        @DisplayName("should use tier-based limiting when enabled")
        void shouldUseTierBasedLimitingWhenEnabled() throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("GET", "/api/users");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String clientIp = "192.168.1.10";
            RateLimitTier tier = RateLimitTier.FREE;
            TierConfig tierConfig = new TierConfig(50, 100);

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            ConsumptionProbe probe = createConsumedProbe(99);

            when(clientIpResolver.resolveClientIp(request)).thenReturn(clientIp);
            when(tierResolver.resolve(authentication, request)).thenReturn(tier);
            when(tierResolver.getUserIdentifier(authentication, clientIp)).thenReturn("user:123");
            when(rateLimitService.tryConsumeForTierAndReturnRemaining("user:123", tier))
                    .thenReturn(probe);
            when(rateLimitService.getTierConfig(tier)).thenReturn(tierConfig);

            // When
            tierFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
            assertThat(response.getHeader("X-RateLimit-Tier")).isEqualTo("free");

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("should block and include tier info when limit exceeded")
        void shouldBlockAndIncludeTierInfoWhenLimitExceeded() throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("GET", "/api/users");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String clientIp = "192.168.1.11";
            RateLimitTier tier = RateLimitTier.ANONYMOUS;
            TierConfig tierConfig = new TierConfig(10, 20);
            long waitNanos = 3_000_000_000L; // 3 seconds

            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            ConsumptionProbe probe = createRejectedProbe(waitNanos);

            when(clientIpResolver.resolveClientIp(request)).thenReturn(clientIp);
            when(tierResolver.resolve(authentication, request)).thenReturn(tier);
            when(tierResolver.getUserIdentifier(authentication, clientIp))
                    .thenReturn("ip:" + clientIp);
            when(rateLimitService.tryConsumeForTierAndReturnRemaining("ip:" + clientIp, tier))
                    .thenReturn(probe);
            when(rateLimitService.getTierConfig(tier)).thenReturn(tierConfig);

            // When
            tierFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, never()).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isEqualTo("3");
            assertThat(response.getHeader("X-RateLimit-Tier")).isEqualTo("anonymous");
            assertThat(response.getContentAsString()).contains("anonymous");
            assertThat(response.getContentAsString()).contains("upgradeUrl");

            SecurityContextHolder.clearContext();
        }
    }

    @Nested
    @DisplayName("Rate limit headers")
    class RateLimitHeadersTests {

        @Test
        @DisplayName("should set all required rate limit headers")
        void shouldSetAllRequiredHeaders() throws Exception {
            // Given
            MockHttpServletRequest request = createRequest("GET", "/api/users");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String clientIp = "192.168.1.20";

            ConsumptionProbe probe = mock(ConsumptionProbe.class);
            lenient().when(probe.isConsumed()).thenReturn(true);
            lenient().when(probe.getRemainingTokens()).thenReturn(100L);
            lenient().when(probe.getNanosToWaitForRefill()).thenReturn(1_000_000_000L);

            when(clientIpResolver.resolveClientIp(request)).thenReturn(clientIp);
            when(rateLimitService.tryConsumeAndReturnRemaining(clientIp, false)).thenReturn(probe);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getHeader("X-RateLimit-Limit")).isNotNull();
            assertThat(response.getHeader("X-RateLimit-Remaining")).isNotNull();
            assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
        }
    }
}
