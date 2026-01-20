package com.jnzader.apigen.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.ManagedChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GrpcChannelFactory Tests")
class GrpcChannelFactoryTest {

    private GrpcChannelFactory factory;

    @BeforeEach
    void setUp() {
        factory = new GrpcChannelFactory();
    }

    @AfterEach
    void tearDown() {
        factory.shutdownAll(1, TimeUnit.SECONDS);
    }

    @Nested
    @DisplayName("Channel Creation")
    class ChannelCreationTests {

        @Test
        @DisplayName("should create channel for target")
        void shouldCreateChannelForTarget() {
            ManagedChannel channel = factory.getChannel("localhost:9090", true);

            assertThat(channel).isNotNull();
            assertThat(channel.isShutdown()).isFalse();
        }

        @Test
        @DisplayName("should reuse existing channel")
        void shouldReuseExistingChannel() {
            ManagedChannel channel1 = factory.getChannel("localhost:9090", true);
            ManagedChannel channel2 = factory.getChannel("localhost:9090", true);

            assertThat(channel1).isSameAs(channel2);
        }

        @Test
        @DisplayName("should create different channels for different targets")
        void shouldCreateDifferentChannels() {
            ManagedChannel channel1 = factory.getChannel("localhost:9090", true);
            ManagedChannel channel2 = factory.getChannel("localhost:9091", true);

            assertThat(channel1).isNotSameAs(channel2);
        }

        @Test
        @DisplayName("should track active channel count")
        void shouldTrackActiveChannelCount() {
            assertThat(factory.getActiveChannelCount()).isZero();

            factory.getChannel("localhost:9090", true);
            assertThat(factory.getActiveChannelCount()).isEqualTo(1);

            factory.getChannel("localhost:9091", true);
            assertThat(factory.getActiveChannelCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Channel Shutdown")
    class ChannelShutdownTests {

        @Test
        @DisplayName("should shutdown specific channel")
        void shouldShutdownSpecificChannel() {
            factory.getChannel("localhost:9090", true);
            factory.getChannel("localhost:9091", true);

            factory.shutdown("localhost:9090");

            assertThat(factory.getActiveChannelCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should shutdown all channels")
        void shouldShutdownAllChannels() {
            factory.getChannel("localhost:9090", true);
            factory.getChannel("localhost:9091", true);

            factory.shutdownAll(1, TimeUnit.SECONDS);

            assertThat(factory.getActiveChannelCount()).isZero();
        }

        @Test
        @DisplayName("should handle shutdown of non-existent channel")
        void shouldHandleNonExistentChannel() {
            factory.shutdown("localhost:9999");

            assertThat(factory.getActiveChannelCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Custom Interceptors")
    class CustomInterceptorsTests {

        @Test
        @DisplayName("should create factory with default interceptors")
        void shouldCreateFactoryWithDefaultInterceptors() {
            GrpcChannelFactory factoryWithInterceptors = new GrpcChannelFactory(List.of());

            assertThat(factoryWithInterceptors).isNotNull();
            factoryWithInterceptors.shutdownAll(1, TimeUnit.SECONDS);
        }
    }
}
