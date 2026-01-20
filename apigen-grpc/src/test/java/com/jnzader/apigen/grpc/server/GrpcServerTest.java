package com.jnzader.apigen.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.grpc.interceptor.LoggingServerInterceptor;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GrpcServer Tests")
class GrpcServerTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build server with port")
        void shouldBuildServerWithPort() {
            GrpcServer server = GrpcServer.builder(9090).build();

            assertThat(server).isNotNull();
            assertThat(server.getPort()).isEqualTo(9090);
        }

        @Test
        @DisplayName("should build server with interceptors")
        void shouldBuildServerWithInterceptors() {
            GrpcServer server =
                    GrpcServer.builder(9091).addInterceptor(new LoggingServerInterceptor()).build();

            assertThat(server).isNotNull();
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class LifecycleTests {

        @Test
        @DisplayName("should start and stop server")
        void shouldStartAndStopServer() throws IOException {
            GrpcServer server = GrpcServer.builder(0).build(); // Port 0 for random available port

            server.start();
            assertThat(server.isRunning()).isTrue();

            boolean terminated = server.shutdown(5, TimeUnit.SECONDS);
            assertThat(terminated).isTrue();
            assertThat(server.isRunning()).isFalse();
        }

        @Test
        @DisplayName("should force shutdown")
        void shouldForceShutdown() throws IOException {
            GrpcServer server = GrpcServer.builder(0).build();

            server.start();
            assertThat(server.isRunning()).isTrue();

            server.shutdownNow();
            assertThat(server.isRunning()).isFalse();
        }
    }

    @Nested
    @DisplayName("Server Access")
    class ServerAccessTests {

        @Test
        @DisplayName("should provide access to underlying server")
        void shouldProvideAccessToUnderlyingServer() {
            GrpcServer server = GrpcServer.builder(9092).build();

            assertThat(server.getServer()).isNotNull();
        }
    }
}
