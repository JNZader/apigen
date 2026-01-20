package com.jnzader.apigen.grpc.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.util.function.Supplier;

/**
 * Client interceptor that adds authentication token to outgoing requests.
 *
 * <p>Retrieves the token from a supplier and adds it as a Bearer token in the Authorization header.
 */
public class AuthenticationClientInterceptor implements ClientInterceptor {

    public static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private static final String BEARER_PREFIX = "Bearer ";

    private final Supplier<String> tokenSupplier;

    /**
     * Creates an authentication interceptor with a static token.
     *
     * @param token the token to use for all requests
     */
    public AuthenticationClientInterceptor(String token) {
        this(() -> token);
    }

    /**
     * Creates an authentication interceptor with a token supplier.
     *
     * @param tokenSupplier supplier that provides the current token
     */
    public AuthenticationClientInterceptor(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    @SuppressWarnings("java:S119") // ReqT/RespT are standard gRPC generic type names
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String token = tokenSupplier.get();
                if (token != null && !token.isBlank()) {
                    headers.put(AUTHORIZATION_KEY, BEARER_PREFIX + token);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
