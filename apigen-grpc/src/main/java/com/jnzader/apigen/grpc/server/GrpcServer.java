package com.jnzader.apigen.grpc.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for managing a gRPC server lifecycle.
 *
 * <p>Provides a fluent builder API for configuring and starting a gRPC server.
 */
public class GrpcServer {

    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    private final Server server;
    private final int port;

    private GrpcServer(Server server, int port) {
        this.server = server;
        this.port = port;
    }

    /**
     * Starts the gRPC server.
     *
     * @return this server instance
     * @throws IOException if the server fails to start
     */
    public GrpcServer start() throws IOException {
        server.start();
        log.info("gRPC server started on port {}", port);
        return this;
    }

    /**
     * Blocks until the server shuts down.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    public void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }

    /**
     * Shuts down the server gracefully.
     *
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @return true if the server terminated within the timeout
     */
    public boolean shutdown(long timeout, TimeUnit unit) {
        log.info("Shutting down gRPC server...");
        server.shutdown();
        try {
            if (server.awaitTermination(timeout, unit)) {
                log.info("gRPC server stopped gracefully");
                return true;
            } else {
                log.warn("gRPC server did not terminate in time, forcing shutdown");
                server.shutdownNow();
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
            return false;
        }
    }

    /** Shuts down the server immediately. */
    public void shutdownNow() {
        server.shutdownNow();
        log.info("gRPC server stopped forcefully");
    }

    /**
     * Checks if the server is running.
     *
     * @return true if the server is running
     */
    public boolean isRunning() {
        return !server.isShutdown() && !server.isTerminated();
    }

    /**
     * Gets the port the server is listening on.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the underlying gRPC server.
     *
     * @return the server instance
     */
    public Server getServer() {
        return server;
    }

    /**
     * Creates a new builder for configuring a gRPC server.
     *
     * @param port the port to listen on
     * @return a new builder
     */
    public static Builder builder(int port) {
        return new Builder(port);
    }

    /** Builder for creating a GrpcServer. */
    public static class Builder {
        private final int port;
        private final List<BindableService> services = new ArrayList<>();
        private final List<ServerInterceptor> interceptors = new ArrayList<>();

        private Builder(int port) {
            this.port = port;
        }

        /**
         * Adds a service to the server.
         *
         * @param service the service to add
         * @return this builder
         */
        public Builder addService(BindableService service) {
            services.add(service);
            return this;
        }

        /**
         * Adds multiple services to the server.
         *
         * @param services the services to add
         * @return this builder
         */
        public Builder addServices(List<BindableService> services) {
            this.services.addAll(services);
            return this;
        }

        /**
         * Adds an interceptor to the server.
         *
         * @param interceptor the interceptor to add
         * @return this builder
         */
        public Builder addInterceptor(ServerInterceptor interceptor) {
            interceptors.add(interceptor);
            return this;
        }

        /**
         * Adds multiple interceptors to the server.
         *
         * @param interceptors the interceptors to add
         * @return this builder
         */
        public Builder addInterceptors(List<ServerInterceptor> interceptors) {
            this.interceptors.addAll(interceptors);
            return this;
        }

        /**
         * Builds the gRPC server.
         *
         * @return the configured server
         */
        public GrpcServer build() {
            ServerBuilder<?> builder = ServerBuilder.forPort(port);

            for (ServerInterceptor interceptor : interceptors) {
                builder.intercept(interceptor);
            }

            for (BindableService service : services) {
                builder.addService(service);
            }

            return new GrpcServer(builder.build(), port);
        }
    }
}
