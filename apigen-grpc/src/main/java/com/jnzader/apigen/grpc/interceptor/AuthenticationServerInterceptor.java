package com.jnzader.apigen.grpc.interceptor;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server interceptor that extracts and validates authentication tokens from metadata.
 *
 * <p>Supports Bearer token authentication via the Authorization header.
 */
public class AuthenticationServerInterceptor implements ServerInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(AuthenticationServerInterceptor.class);

    public static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    public static final Context.Key<String> USER_ID_KEY = Context.key("user-id");
    public static final Context.Key<Set<String>> ROLES_KEY = Context.key("roles");
    public static final Context.Key<String> TOKEN_KEY = Context.key("token");

    private static final String BEARER_PREFIX = "Bearer ";

    private final Function<String, AuthResult> tokenValidator;
    private final Set<String> excludedMethods;

    /**
     * Creates an authentication interceptor with the given token validator.
     *
     * @param tokenValidator function that validates a token and returns auth result
     */
    public AuthenticationServerInterceptor(Function<String, AuthResult> tokenValidator) {
        this(tokenValidator, Set.of());
    }

    /**
     * Creates an authentication interceptor with excluded methods.
     *
     * @param tokenValidator function that validates a token and returns auth result
     * @param excludedMethods method names to exclude from authentication
     */
    public AuthenticationServerInterceptor(
            Function<String, AuthResult> tokenValidator, Set<String> excludedMethods) {
        this.tokenValidator = tokenValidator;
        this.excludedMethods = excludedMethods;
    }

    @Override
    @SuppressWarnings("java:S119") // ReqT/RespT are standard gRPC generic type names
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();

        // Skip authentication for excluded methods
        if (excludedMethods.contains(methodName)) {
            return next.startCall(call, headers);
        }

        String authHeader = headers.get(AUTHORIZATION_KEY);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("Missing or invalid authorization header for method: {}", methodName);
            call.close(
                    Status.UNAUTHENTICATED.withDescription("Missing or invalid authorization"),
                    new Metadata());
            return new ServerCall.Listener<>() {};
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            AuthResult authResult = tokenValidator.apply(token);

            if (!authResult.isValid()) {
                log.debug(
                        "Invalid token for method: {} - {}", methodName, authResult.errorMessage());
                call.close(
                        Status.UNAUTHENTICATED.withDescription(authResult.errorMessage()),
                        new Metadata());
                return new ServerCall.Listener<>() {};
            }

            Context context =
                    Context.current()
                            .withValue(USER_ID_KEY, authResult.userId())
                            .withValue(ROLES_KEY, authResult.roles())
                            .withValue(TOKEN_KEY, token);

            return Contexts.interceptCall(context, call, headers, next);

        } catch (Exception e) {
            log.error("Error validating token for method: {}", methodName, e);
            call.close(
                    Status.UNAUTHENTICATED.withDescription("Token validation failed"),
                    new Metadata());
            return new ServerCall.Listener<>() {};
        }
    }

    /** Result of token authentication. */
    public record AuthResult(
            boolean isValid, String userId, Set<String> roles, String errorMessage) {

        public static AuthResult success(String userId, Set<String> roles) {
            return new AuthResult(true, userId, roles, null);
        }

        public static AuthResult failure(String errorMessage) {
            return new AuthResult(false, null, Set.of(), errorMessage);
        }
    }
}
