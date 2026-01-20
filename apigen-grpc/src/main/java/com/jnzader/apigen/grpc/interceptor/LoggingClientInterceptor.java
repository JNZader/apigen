package com.jnzader.apigen.grpc.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client interceptor that logs gRPC calls with timing information.
 *
 * <p>Logs method name, duration, and status for each outgoing call.
 */
public class LoggingClientInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingClientInterceptor.class);

    @Override
    @SuppressWarnings("java:S119") // ReqT/RespT are standard gRPC generic type names
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        String methodName = method.getFullMethodName();
        long startTime = System.nanoTime();

        log.debug("gRPC client call started: {}", methodName);

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(
                        new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(
                                responseListener) {
                            @Override
                            public void onClose(Status status, Metadata trailers) {
                                long duration = (System.nanoTime() - startTime) / 1_000_000;
                                if (status.isOk()) {
                                    log.debug(
                                            "gRPC client call completed: {} in {}ms",
                                            methodName,
                                            duration);
                                } else {
                                    log.warn(
                                            "gRPC client call failed: {} in {}ms - Status: {} - {}",
                                            methodName,
                                            duration,
                                            status.getCode(),
                                            status.getDescription());
                                }
                                super.onClose(status, trailers);
                            }

                            @Override
                            public void onMessage(RespT message) {
                                log.trace("gRPC response received: {}", methodName);
                                super.onMessage(message);
                            }
                        },
                        headers);
            }

            @Override
            public void sendMessage(ReqT message) {
                log.trace("gRPC request sent: {}", methodName);
                super.sendMessage(message);
            }
        };
    }
}
