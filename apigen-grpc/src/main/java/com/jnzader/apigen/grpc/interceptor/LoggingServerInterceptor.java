package com.jnzader.apigen.grpc.interceptor;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server interceptor that logs gRPC calls with timing information.
 *
 * <p>Logs method name, duration, and status for each call.
 */
public class LoggingServerInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingServerInterceptor.class);

    @Override
    @SuppressWarnings("java:S119") // ReqT/RespT are standard gRPC generic type names
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        long startTime = System.nanoTime();

        log.debug("gRPC call started: {}", methodName);

        ServerCall<ReqT, RespT> wrappedCall =
                new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        long duration = (System.nanoTime() - startTime) / 1_000_000;
                        if (status.isOk()) {
                            log.debug("gRPC call completed: {} in {}ms", methodName, duration);
                        } else {
                            log.warn(
                                    "gRPC call failed: {} in {}ms - Status: {} - {}",
                                    methodName,
                                    duration,
                                    status.getCode(),
                                    status.getDescription());
                        }
                        super.close(status, trailers);
                    }
                };

        ServerCall.Listener<ReqT> listener = next.startCall(wrappedCall, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onMessage(ReqT message) {
                log.trace("gRPC request received: {}", methodName);
                super.onMessage(message);
            }

            @Override
            public void onCancel() {
                log.debug("gRPC call cancelled: {}", methodName);
                super.onCancel();
            }
        };
    }
}
