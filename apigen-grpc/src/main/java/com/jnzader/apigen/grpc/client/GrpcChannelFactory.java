package com.jnzader.apigen.grpc.client;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating and managing gRPC client channels.
 *
 * <p>Channels are cached by target address and can be reused across multiple stubs.
 */
public class GrpcChannelFactory {

    private static final Logger log = LoggerFactory.getLogger(GrpcChannelFactory.class);

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
    private final List<ClientInterceptor> defaultInterceptors;

    public GrpcChannelFactory() {
        this(List.of());
    }

    public GrpcChannelFactory(List<ClientInterceptor> defaultInterceptors) {
        this.defaultInterceptors = new ArrayList<>(defaultInterceptors);
    }

    /**
     * Gets or creates a channel for the given target.
     *
     * @param target the target address (host:port)
     * @return the managed channel
     */
    public ManagedChannel getChannel(String target) {
        return getChannel(target, false);
    }

    /**
     * Gets or creates a channel for the given target.
     *
     * @param target the target address (host:port)
     * @param usePlaintext whether to use plaintext (no TLS)
     * @return the managed channel
     */
    public ManagedChannel getChannel(String target, boolean usePlaintext) {
        return channels.computeIfAbsent(
                target,
                t -> {
                    log.debug("Creating new gRPC channel for target: {}", target);
                    return createChannel(t, usePlaintext, List.of());
                });
    }

    /**
     * Creates a new channel with custom interceptors.
     *
     * @param target the target address
     * @param usePlaintext whether to use plaintext
     * @param interceptors additional interceptors
     * @return the managed channel
     */
    public ManagedChannel createChannel(
            String target, boolean usePlaintext, List<ClientInterceptor> interceptors) {

        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forTarget(target);

        if (usePlaintext) {
            builder.usePlaintext();
        }

        // Add default interceptors
        for (ClientInterceptor interceptor : defaultInterceptors) {
            builder.intercept(interceptor);
        }

        // Add custom interceptors
        for (ClientInterceptor interceptor : interceptors) {
            builder.intercept(interceptor);
        }

        return builder.build();
    }

    /**
     * Shuts down all managed channels gracefully.
     *
     * @param timeout the timeout value
     * @param unit the timeout unit
     */
    public void shutdownAll(long timeout, TimeUnit unit) {
        log.info("Shutting down {} gRPC channels", channels.size());
        for (Map.Entry<String, ManagedChannel> entry : channels.entrySet()) {
            try {
                ManagedChannel channel = entry.getValue();
                channel.shutdown();
                if (!channel.awaitTermination(timeout, unit)) {
                    log.warn("Channel {} did not terminate in time", entry.getKey());
                    channel.shutdownNow();
                }
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                entry.getValue().shutdownNow();
            }
        }
        channels.clear();
    }

    /**
     * Shuts down a specific channel.
     *
     * @param target the target address of the channel to shut down
     */
    public void shutdown(String target) {
        ManagedChannel channel = channels.remove(target);
        if (channel != null) {
            log.debug("Shutting down gRPC channel for target: {}", target);
            channel.shutdown();
        }
    }

    /**
     * Gets the number of active channels.
     *
     * @return the number of channels
     */
    public int getActiveChannelCount() {
        return channels.size();
    }
}
