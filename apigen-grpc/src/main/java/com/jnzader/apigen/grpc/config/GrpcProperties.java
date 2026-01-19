package com.jnzader.apigen.grpc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for APiGen gRPC support. */
@ConfigurationProperties(prefix = "apigen.grpc")
public class GrpcProperties {

    /** Whether gRPC support is enabled. */
    private boolean enabled = false;

    /** Server configuration. */
    private Server server = new Server();

    /** Client configuration. */
    private Client client = new Client();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public static class Server {
        /** Server port (default: 9090). */
        private int port = 9090;

        /** Whether to enable server-side logging. */
        private Logging logging = new Logging();

        /** Maximum inbound message size in bytes. */
        private int maxInboundMessageSize = 4 * 1024 * 1024;

        /** Maximum metadata size in bytes. */
        private int maxInboundMetadataSize = 8192;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public Logging getLogging() {
            return logging;
        }

        public void setLogging(Logging logging) {
            this.logging = logging;
        }

        public int getMaxInboundMessageSize() {
            return maxInboundMessageSize;
        }

        public void setMaxInboundMessageSize(int maxInboundMessageSize) {
            this.maxInboundMessageSize = maxInboundMessageSize;
        }

        public int getMaxInboundMetadataSize() {
            return maxInboundMetadataSize;
        }

        public void setMaxInboundMetadataSize(int maxInboundMetadataSize) {
            this.maxInboundMetadataSize = maxInboundMetadataSize;
        }
    }

    public static class Client {
        /** Default deadline in milliseconds. */
        private long deadlineMs = 10000;

        /** Whether to enable client-side logging. */
        private Logging logging = new Logging();

        /** Whether to use plaintext (no TLS) by default. */
        private boolean usePlaintext = false;

        public long getDeadlineMs() {
            return deadlineMs;
        }

        public void setDeadlineMs(long deadlineMs) {
            this.deadlineMs = deadlineMs;
        }

        public Logging getLogging() {
            return logging;
        }

        public void setLogging(Logging logging) {
            this.logging = logging;
        }

        public boolean isUsePlaintext() {
            return usePlaintext;
        }

        public void setUsePlaintext(boolean usePlaintext) {
            this.usePlaintext = usePlaintext;
        }
    }

    public static class Logging {
        /** Whether logging is enabled. */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
