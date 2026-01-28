package com.jnzader.apigen.security.infrastructure.exception;

import com.jnzader.apigen.core.domain.exception.AccountLockedException;
import com.jnzader.apigen.core.domain.exception.AuthenticationException;
import com.jnzader.apigen.core.domain.exception.InvalidTokenException;
import com.jnzader.apigen.core.domain.exception.RateLimitExceededException;
import com.jnzader.apigen.core.domain.exception.TokenExpiredException;
import com.jnzader.apigen.core.infrastructure.exception.ProblemDetail;
import com.jnzader.apigen.security.domain.exception.SecurityAccountLockedException;
import com.jnzader.apigen.security.domain.exception.SecurityInvalidTokenException;
import com.jnzader.apigen.security.domain.exception.SecurityRateLimitException;
import com.jnzader.apigen.security.domain.exception.SecurityTokenExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Exception handler for security-related exceptions.
 *
 * <p>Handles authentication, authorization, rate limiting, and account lockout errors with RFC 7807
 * compliant responses.
 */
@ControllerAdvice(basePackages = "com.jnzader.apigen.security")
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class SecurityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);
    private static final MediaType APPLICATION_PROBLEM_JSON =
            MediaType.valueOf("application/problem+json");

    /** Handle invalid token exceptions. */
    @ExceptionHandler(SecurityInvalidTokenException.class)
    public ResponseEntity<ProblemDetail> handleSecurityInvalidTokenException(
            SecurityInvalidTokenException ex, HttpServletRequest request) {
        log.debug("Invalid token: {} - {}", ex.getReason(), ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:invalid-token"))
                        .title("Invalid Token")
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("errorCode", ex.getErrorCode())
                        .extension("reason", ex.getReason().name())
                        .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handle invalid token exceptions from core. */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTokenException(
            InvalidTokenException ex, HttpServletRequest request) {
        log.debug("Invalid token: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:invalid-token"))
                        .title("Invalid Token")
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("errorCode", ex.getErrorCode())
                        .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handle expired token exceptions. */
    @ExceptionHandler(SecurityTokenExpiredException.class)
    public ResponseEntity<ProblemDetail> handleSecurityTokenExpiredException(
            SecurityTokenExpiredException ex, HttpServletRequest request) {
        log.debug("Token expired: {}", ex.getMessage());

        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:token-expired"))
                        .title("Token Expired")
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("errorCode", ex.getErrorCode())
                        .extension("tokenType", ex.getTokenType());

        if (ex.getExpiredAt() != null) {
            builder.extension("expiredAt", ex.getExpiredAt().toString());
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(builder.build());
    }

    /** Handle expired token exceptions from core. */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ProblemDetail> handleTokenExpiredException(
            TokenExpiredException ex, HttpServletRequest request) {
        log.debug("Token expired: {}", ex.getMessage());

        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:token-expired"))
                        .title("Token Expired")
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("errorCode", ex.getErrorCode());

        if (ex.getExpiredAt() != null) {
            builder.extension("expiredAt", ex.getExpiredAt().toString());
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(builder.build());
    }

    /** Handle generic authentication exceptions. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.debug("Authentication failed: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:authentication-failed"))
                        .title("Authentication Failed")
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("errorCode", ex.getErrorCode())
                        .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handle security rate limit exceptions. */
    @ExceptionHandler(SecurityRateLimitException.class)
    public ResponseEntity<ProblemDetail> handleSecurityRateLimitException(
            SecurityRateLimitException ex, HttpServletRequest request) {
        log.warn(
                "Rate limit exceeded for {} endpoint: {}",
                ex.getEndpointType().getDisplayName(),
                ex.getMessage());

        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:rate-limit-exceeded"))
                        .title("Too Many Requests")
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("retryAfterSeconds", ex.getRetryAfterSeconds())
                        .extension("endpointType", ex.getEndpointType().name());

        if (ex.getTier() != null) {
            builder.extension("tier", ex.getTier());
            builder.extension("limit", ex.getLimit());
        }

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(builder.build());
    }

    /** Handle rate limit exceptions from core. */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceededException(
            RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:rate-limit-exceeded"))
                        .title("Too Many Requests")
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("retryAfterSeconds", ex.getRetryAfterSeconds());

        if (ex.getTier() != null) {
            builder.extension("tier", ex.getTier());
            builder.extension("limit", ex.getLimit());
        }

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(builder.build());
    }

    /** Handle security account locked exceptions. */
    @ExceptionHandler(SecurityAccountLockedException.class)
    public ResponseEntity<ProblemDetail> handleSecurityAccountLockedException(
            SecurityAccountLockedException ex, HttpServletRequest request) {
        log.warn("Account locked ({}): {}", ex.getLockReason().getDescription(), ex.getMessage());

        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:account-locked"))
                        .title("Account Locked")
                        .status(HttpStatus.LOCKED.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("lockReason", ex.getLockReason().name())
                        .extension("remainingSeconds", ex.getRemainingSeconds());

        if (ex.getUnlockTime() != null) {
            builder.extension("unlockTime", ex.getUnlockTime().toString());
        }
        if (ex.getFailedAttempts() > 0) {
            builder.extension("failedAttempts", ex.getFailedAttempts());
        }

        return ResponseEntity.status(HttpStatus.LOCKED)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(builder.build());
    }

    /** Handle account locked exceptions from core. */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ProblemDetail> handleAccountLockedException(
            AccountLockedException ex, HttpServletRequest request) {
        log.warn("Account locked: {}", ex.getMessage());

        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:account-locked"))
                        .title("Account Locked")
                        .status(HttpStatus.LOCKED.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("remainingSeconds", ex.getRemainingSeconds());

        if (ex.getUnlockTime() != null) {
            builder.extension("unlockTime", ex.getUnlockTime().toString());
        }

        return ResponseEntity.status(HttpStatus.LOCKED)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(builder.build());
    }
}
