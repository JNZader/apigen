package com.jnzader.apigen.security.infrastructure.audit;

import com.jnzader.apigen.security.infrastructure.audit.SecurityAuditEvent.SecurityEventType;
import com.jnzader.apigen.security.infrastructure.audit.SecurityAuditEvent.SecurityOutcome;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("SecurityAuditService Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityAuditServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletRequestAttributes requestAttributes;

    @Captor
    private ArgumentCaptor<SecurityAuditEvent> eventCaptor;

    private SecurityAuditService auditService;
    private MockedStatic<RequestContextHolder> requestContextHolderMock;
    private SecurityContext originalSecurityContext;

    @BeforeEach
    void setUp() {
        auditService = new SecurityAuditService(eventPublisher);
        originalSecurityContext = SecurityContextHolder.getContext();

        requestContextHolderMock = mockStatic(RequestContextHolder.class);
        requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);
        when(requestAttributes.getRequest()).thenReturn(request);
    }

    @AfterEach
    void tearDown() {
        requestContextHolderMock.close();
        SecurityContextHolder.setContext(originalSecurityContext);
    }

    @Nested
    @DisplayName("logAuthenticationSuccess")
    class LogAuthenticationSuccessTests {

        @Test
        @DisplayName("should log authentication success event")
        void shouldLogAuthenticationSuccessEvent() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");

            auditService.logAuthenticationSuccess("testuser");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            SecurityAuditEvent event = eventCaptor.getValue();

            assertThat(event.eventType()).isEqualTo(SecurityEventType.AUTHENTICATION_SUCCESS);
            assertThat(event.username()).isEqualTo("testuser");
            assertThat(event.ipAddress()).isEqualTo("192.168.1.1");
            assertThat(event.userAgent()).isEqualTo("TestBrowser/1.0");
            assertThat(event.action()).isEqualTo("LOGIN");
            assertThat(event.outcome()).isEqualTo(SecurityOutcome.SUCCESS);
        }

        @Test
        @DisplayName("should use X-Forwarded-For for IP")
        void shouldUseXForwardedForForIp() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");
            when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");

            auditService.logAuthenticationSuccess("testuser");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().ipAddress()).isEqualTo("10.0.0.1");
        }
    }

    @Nested
    @DisplayName("logAuthenticationFailure")
    class LogAuthenticationFailureTests {

        @Test
        @DisplayName("should log authentication failure event with reason")
        void shouldLogAuthenticationFailureEventWithReason() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.2");
            when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");

            auditService.logAuthenticationFailure("testuser", "Invalid password");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            SecurityAuditEvent event = eventCaptor.getValue();

            assertThat(event.eventType()).isEqualTo(SecurityEventType.AUTHENTICATION_FAILURE);
            assertThat(event.username()).isEqualTo("testuser");
            assertThat(event.outcome()).isEqualTo(SecurityOutcome.FAILURE);
            assertThat(event.details()).containsEntry("reason", "Invalid password");
        }
    }

    @Nested
    @DisplayName("logAccessDenied")
    class LogAccessDeniedTests {

        @Test
        @DisplayName("should log access denied event for authenticated user")
        void shouldLogAccessDeniedEventForAuthenticatedUser() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.3");
            when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");
            setUpAuthenticatedUser("admin");

            auditService.logAccessDenied("/api/admin/users", "ROLE_SUPER_ADMIN");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            SecurityAuditEvent event = eventCaptor.getValue();

            assertThat(event.eventType()).isEqualTo(SecurityEventType.ACCESS_DENIED);
            assertThat(event.username()).isEqualTo("admin");
            assertThat(event.resource()).isEqualTo("/api/admin/users");
            assertThat(event.outcome()).isEqualTo(SecurityOutcome.DENIED);
            assertThat(event.details()).containsEntry("requiredAuthority", "ROLE_SUPER_ADMIN");
        }

        @Test
        @DisplayName("should use anonymous for unauthenticated user")
        void shouldUseAnonymousForUnauthenticatedUser() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.4");
            when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");

            auditService.logAccessDenied("/api/secure", "ROLE_USER");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().username()).isEqualTo("anonymous");
        }
    }

    @Nested
    @DisplayName("logRateLimitExceeded")
    class LogRateLimitExceededTests {

        @Test
        @DisplayName("should log rate limit exceeded event")
        void shouldLogRateLimitExceededEvent() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.5");
            when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");

            auditService.logRateLimitExceeded("/api/users");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            SecurityAuditEvent event = eventCaptor.getValue();

            assertThat(event.eventType()).isEqualTo(SecurityEventType.RATE_LIMIT_EXCEEDED);
            assertThat(event.resource()).isEqualTo("/api/users");
            assertThat(event.action()).isEqualTo("REQUEST");
            assertThat(event.outcome()).isEqualTo(SecurityOutcome.BLOCKED);
        }
    }

    @Nested
    @DisplayName("logResourceAccess")
    class LogResourceAccessTests {

        @Test
        @DisplayName("should log resource access event")
        void shouldLogResourceAccessEvent() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.6");
            when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");
            setUpAuthenticatedUser("user1");

            auditService.logResourceAccess("/api/users/123", "GET", SecurityOutcome.SUCCESS);

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            SecurityAuditEvent event = eventCaptor.getValue();

            assertThat(event.eventType()).isEqualTo(SecurityEventType.RESOURCE_ACCESS);
            assertThat(event.username()).isEqualTo("user1");
            assertThat(event.resource()).isEqualTo("/api/users/123");
            assertThat(event.action()).isEqualTo("GET");
            assertThat(event.outcome()).isEqualTo(SecurityOutcome.SUCCESS);
        }
    }

    @Nested
    @DisplayName("logAdminAction")
    class LogAdminActionTests {

        @Test
        @DisplayName("should log admin action event")
        void shouldLogAdminActionEvent() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.7");
            when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");
            setUpAuthenticatedUser("superadmin");

            Map<String, Object> details = Map.of("targetUserId", 42, "newRole", "ADMIN");
            auditService.logAdminAction("ROLE_CHANGE", "/api/users/42", details);

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            SecurityAuditEvent event = eventCaptor.getValue();

            assertThat(event.eventType()).isEqualTo(SecurityEventType.ADMIN_ACTION);
            assertThat(event.username()).isEqualTo("superadmin");
            assertThat(event.action()).isEqualTo("ROLE_CHANGE");
            assertThat(event.resource()).isEqualTo("/api/users/42");
            assertThat(event.outcome()).isEqualTo(SecurityOutcome.SUCCESS);
            assertThat(event.details()).containsEntry("targetUserId", 42);
        }

        @Test
        @DisplayName("should use unknown for unauthenticated admin action")
        void shouldUseUnknownForUnauthenticatedAdminAction() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.8");
            when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");

            auditService.logAdminAction("SYSTEM_CONFIG", "/config", Map.of());

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().username()).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("logSuspiciousActivity")
    class LogSuspiciousActivityTests {

        @Test
        @DisplayName("should log suspicious activity event")
        void shouldLogSuspiciousActivityEvent() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.9");
            when(request.getHeader("User-Agent")).thenReturn("SuspiciousBot/1.0");

            Map<String, Object> details = Map.of(
                    "pattern", "SQL injection attempt",
                    "payload", "1' OR '1'='1"
            );
            auditService.logSuspiciousActivity("SQL injection detected", details);

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            SecurityAuditEvent event = eventCaptor.getValue();

            assertThat(event.eventType()).isEqualTo(SecurityEventType.SUSPICIOUS_ACTIVITY);
            assertThat(event.action()).isEqualTo("SQL injection detected");
            assertThat(event.outcome()).isEqualTo(SecurityOutcome.BLOCKED);
            assertThat(event.details()).containsEntry("pattern", "SQL injection attempt");
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle event publisher exception gracefully")
        void shouldHandleEventPublisherExceptionGracefully() {
            when(request.getRemoteAddr()).thenReturn("192.168.1.10");
            when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");
            doThrow(new RuntimeException("Publisher error")).when(eventPublisher).publishEvent(any());

            // Should not throw exception
            auditService.logAuthenticationSuccess("testuser");

            verify(eventPublisher).publishEvent(any(SecurityAuditEvent.class));
        }

        @Test
        @DisplayName("should handle missing request context")
        void shouldHandleMissingRequestContext() {
            requestContextHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            auditService.logAuthenticationSuccess("testuser");

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().ipAddress()).isEqualTo("unknown");
            assertThat(eventCaptor.getValue().userAgent()).isEqualTo("unknown");
        }
    }

    private void setUpAuthenticatedUser(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username, null, Collections.emptyList()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
