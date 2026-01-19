package com.jnzader.apigen.grpc.interceptor;

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
 */
public class ExceptionHandlingInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlingInterceptor.class);

    private static final Metadata.Key<String> ERROR_TYPE_KEY =
            Metadata.Key.of("error-type", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ERROR_DETAIL_KEY =
            Metadata.Key.of("error-detail", Metadata.ASCII_STRING_MARSHALLER);

    @Override
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
    public Status mapExceptionToStatus(Throwable e) {
        if (e instanceof EntityNotFoundException) {
            return Status.NOT_FOUND.withDescription(e.getMessage()).withCause(e);
        }

        if (e instanceof ConstraintViolationException) {
            return Status.INVALID_ARGUMENT.withDescription("Validation failed").withCause(e);
        }

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
        return metadata;
    }
}
