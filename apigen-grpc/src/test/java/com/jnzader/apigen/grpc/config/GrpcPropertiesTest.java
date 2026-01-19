package com.jnzader.apigen.grpc.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GrpcProperties Tests")
class GrpcPropertiesTest {

    private GrpcProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GrpcProperties();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        @Test
        @DisplayName("should have disabled by default")
        void shouldBeDisabledByDefault() {
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should have default server port")
        void shouldHaveDefaultServerPort() {
            assertThat(properties.getServer().getPort()).isEqualTo(9090);
        }

        @Test
        @DisplayName("should have server logging enabled by default")
        void shouldHaveServerLoggingEnabled() {
            assertThat(properties.getServer().getLogging().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should have default client deadline")
        void shouldHaveDefaultClientDeadline() {
            assertThat(properties.getClient().getDeadlineMs()).isEqualTo(10000);
        }

        @Test
        @DisplayName("should have client logging enabled by default")
        void shouldHaveClientLoggingEnabled() {
            assertThat(properties.getClient().getLogging().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should have plaintext disabled by default")
        void shouldHavePlaintextDisabled() {
            assertThat(properties.getClient().isUsePlaintext()).isFalse();
        }
    }

    @Nested
    @DisplayName("Setters")
    class SettersTests {

        @Test
        @DisplayName("should set enabled")
        void shouldSetEnabled() {
            properties.setEnabled(true);

            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should set server port")
        void shouldSetServerPort() {
            properties.getServer().setPort(8080);

            assertThat(properties.getServer().getPort()).isEqualTo(8080);
        }

        @Test
        @DisplayName("should set server logging")
        void shouldSetServerLogging() {
            properties.getServer().getLogging().setEnabled(false);

            assertThat(properties.getServer().getLogging().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should set client deadline")
        void shouldSetClientDeadline() {
            properties.getClient().setDeadlineMs(5000);

            assertThat(properties.getClient().getDeadlineMs()).isEqualTo(5000);
        }

        @Test
        @DisplayName("should set client plaintext")
        void shouldSetClientPlaintext() {
            properties.getClient().setUsePlaintext(true);

            assertThat(properties.getClient().isUsePlaintext()).isTrue();
        }

        @Test
        @DisplayName("should set max inbound message size")
        void shouldSetMaxInboundMessageSize() {
            properties.getServer().setMaxInboundMessageSize(8 * 1024 * 1024);

            assertThat(properties.getServer().getMaxInboundMessageSize())
                    .isEqualTo(8 * 1024 * 1024);
        }

        @Test
        @DisplayName("should set max inbound metadata size")
        void shouldSetMaxInboundMetadataSize() {
            properties.getServer().setMaxInboundMetadataSize(16384);

            assertThat(properties.getServer().getMaxInboundMetadataSize()).isEqualTo(16384);
        }

        @Test
        @DisplayName("should set server object")
        void shouldSetServerObject() {
            GrpcProperties.Server server = new GrpcProperties.Server();
            server.setPort(7070);
            properties.setServer(server);

            assertThat(properties.getServer().getPort()).isEqualTo(7070);
        }

        @Test
        @DisplayName("should set client object")
        void shouldSetClientObject() {
            GrpcProperties.Client client = new GrpcProperties.Client();
            client.setDeadlineMs(3000);
            properties.setClient(client);

            assertThat(properties.getClient().getDeadlineMs()).isEqualTo(3000);
        }

        @Test
        @DisplayName("should set logging object")
        void shouldSetLoggingObject() {
            GrpcProperties.Logging logging = new GrpcProperties.Logging();
            logging.setEnabled(false);
            properties.getServer().setLogging(logging);

            assertThat(properties.getServer().getLogging().isEnabled()).isFalse();
        }
    }
}
