package com.jnzader.apigen.grpc.interceptor;

import com.jnzader.apigen.core.domain.exception.AccountLockedException;
import com.jnzader.apigen.core.domain.exception.AuthenticationException;
import com.jnzader.apigen.core.domain.exception.DuplicateResourceException;
import com.jnzader.apigen.core.domain.exception.ExternalServiceException;
import com.jnzader.apigen.core.domain.exception.IdMismatchException;
import com.jnzader.apigen.core.domain.exception.OperationFailedException;
import com.jnzader.apigen.core.domain.exception.PreconditionFailedException;
import com.jnzader.apigen.core.domain.exception.RateLimitExceededException;
import com.jnzader.apigen.core.domain.exception.ResourceNotFoundException;
import com.jnzader.apigen.core.domain.exception.UnauthorizedActionException;
import com.jnzader.apigen.core.domain.exception.ValidationException;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server interceptor that converts exceptions to appropriate gRPC status codes.
 *
 * <p>Maps common exceptions to standard gRPC status codes following RFC 7807 semantics.
 *
 * <p>Handles apigen-core exceptions:
 *
 * <ul>
 *   <li>ResourceNotFoundException → NOT_FOUND
 *   <li>ValidationException → INVALID_ARGUMENT
 *   <li>DuplicateResourceException → ALREADY_EXISTS
 *   <li>UnauthorizedActionException → PERMISSION_DENIED
 *   <li>AuthenticationException → UNAUTHENTICATED
 *   <li>PreconditionFailedException → FAILED_PRECONDITION
 *   <li>RateLimitExceededException → RESOURCE_EXHAUSTED
 *   <li>AccountLockedException → PERMISSION_DENIED
 *   <li>ExternalServiceException → UNAVAILABLE
 *   <li>OperationFailedException → INTERNAL
 * </ul>
 *
 * <p>Also handles JPA and standard Java exceptions:
 *
 * <ul>
 *   <li>EntityNotFoundException → NOT_FOUND
 *   <li>ConstraintViolationException → INVALID_ARGUMENT
 *   <li>IllegalArgumentException → INVALID_ARGUMENT
 *   <li>IllegalStateException → FAILED_PRECONDITION
 *   <li>OptimisticLockException → ABORTED
 *   <li>SecurityException → PERMISSION_DENIED
 *   <li>UnsupportedOperationException → UNIMPLEMENTED
 * </ul>
 */
public class ExceptionHandlingInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlingInterceptor.class);

    private static final Metadata.Key<String> ERROR_TYPE_KEY =
            Metadata.Key.of("error-type", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ERROR_DETAIL_KEY =
            Metadata.Key.of("error-detail", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ERROR_CODE_KEY =
            Metadata.Key.of("error-code", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    @SuppressWarnings("java:S119") // ReqT/RespT are standard gRPC generic type names
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Exception e) {
                    handleException(call, e);
                }
            }

            @Override
            public void onMessage(ReqT message) {
                try {
                    super.onMessage(message);
                } catch (Exception e) {
                    handleException(call, e);
                }
            }
        };
    }

    @SuppressWarnings("java:S119") // ReqT/RespT are standard gRPC generic type names
    private <ReqT, RespT> void handleException(ServerCall<ReqT, RespT> call, Exception e) {
        Status status = mapExceptionToStatus(e);
        Metadata trailers = createErrorMetadata(e);

        log.error("gRPC call failed with exception: {}", e.getMessage(), e);

        call.close(status, trailers);
    }

    /**
     * Maps an exception to the appropriate gRPC status.
     *
     * @param e the exception to map
     * @return the corresponding gRPC status
     */
    @SuppressWarnings("java:S3776") // Complexity is acceptable for exception mapping
    public Status mapExceptionToStatus(Throwable e) {
        // APiGen Core Exceptions
        if (e instanceof ResourceNotFoundException) {
            return Status.NOT_FOUND.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof ValidationException) {
            return Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof DuplicateResourceException) {
            return Status.ALREADY_EXISTS.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof UnauthorizedActionException) {
            return Status.PERMISSION_DENIED.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof AuthenticationException) {
            return Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof PreconditionFailedException) {
            return Status.FAILED_PRECONDITION.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof IdMismatchException idEx) {
            return Status.INVALID_ARGUMENT
                    .withDescription(
                            String.format(
                                    "ID mismatch: path ID (%s) does not match body ID (%s)",
                                    idEx.getPathId(), idEx.getBodyId()))
                    .withCause(e);
        }

        if (e instanceof RateLimitExceededException rateEx) {
            return Status.RESOURCE_EXHAUSTED
                    .withDescription(
                            String.format(
                                    "%s. Retry after %d seconds.",
                                    e.getMessage(), rateEx.getRetryAfterSeconds()))
                    .withCause(e);
        }

        if (e instanceof AccountLockedException lockEx) {
            return Status.PERMISSION_DENIED
                    .withDescription(
                            String.format(
                                    "%s. Unlock in %d seconds.",
                                    e.getMessage(), lockEx.getRemainingSeconds()))
                    .withCause(e);
        }

        if (e instanceof ExternalServiceException extEx) {
            return Status.UNAVAILABLE
                    .withDescription(
                            String.format(
                                    "External service '%s' unavailable: %s",
                                    extEx.getServiceName(), e.getMessage()))
                    .withCause(e);
        }

        if (e instanceof OperationFailedException) {
            return Status.INTERNAL.withDescription(e.getMessage()).withCause(e);
        }

        // JPA Exceptions
        if (e instanceof EntityNotFoundException) {
            return Status.NOT_FOUND.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof ConstraintViolationException) {
            return Status.INVALID_ARGUMENT.withDescription("Validation failed").withCause(e);
        }

        // Standard Java Exceptions
        if (e instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof IllegalStateException) {
            return Status.FAILED_PRECONDITION.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof OptimisticLockException) {
            return Status.ABORTED.withDescription("Concurrent modification detected").withCause(e);
        }

        if (e instanceof SecurityException) {
            return Status.PERMISSION_DENIED.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof UnsupportedOperationException) {
            return Status.UNIMPLEMENTED.withDescription(e.getMessage()).withCause(e);
        }

        // Default to internal error
        return Status.INTERNAL.withDescription("Internal server error").withCause(e);
    }

    private Metadata createErrorMetadata(Exception e) {
        Metadata metadata = new Metadata();
        metadata.put(ERROR_TYPE_KEY, e.getClass().getSimpleName());

        if (e.getMessage() != null) {
            metadata.put(ERROR_DETAIL_KEY, e.getMessage());
        }

        // Add error codes for apigen-core exceptions
        if (e instanceof AuthenticationException authEx) {
            metadata.put(ERROR_CODE_KEY, authEx.getErrorCode());
        } else if (e instanceof RateLimitExceededException rateEx && rateEx.getTier() != null) {
            metadata.put(ERROR_CODE_KEY, "RATE_LIMIT_EXCEEDED_" + rateEx.getTier().toUpperCase());
        } else if (e instanceof AccountLockedException) {
            metadata.put(ERROR_CODE_KEY, "ACCOUNT_LOCKED");
        } else if (e instanceof ExternalServiceException extEx) {
            metadata.put(
                    ERROR_CODE_KEY, "EXTERNAL_SERVICE_" + extEx.getServiceName().toUpperCase());
        }

        return metadata;
    }
}
