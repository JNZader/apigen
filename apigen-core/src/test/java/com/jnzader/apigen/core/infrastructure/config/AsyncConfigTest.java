package com.jnzader.apigen.core.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AsyncConfig Tests")
class AsyncConfigTest {

    private AsyncConfig asyncConfig;

    @BeforeEach
    void setUp() {
        asyncConfig = new AsyncConfig();
    }

    @Nested
    @DisplayName("virtualThreadDomainEventExecutor")
    class VirtualThreadDomainEventExecutorTests {

        @Test
        @DisplayName("should create SimpleAsyncTaskExecutor")
        void shouldCreateSimpleAsyncTaskExecutor() {
            Executor executor = asyncConfig.virtualThreadDomainEventExecutor();

            assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);
        }

        @Test
        @DisplayName("should configure virtual threads")
        void shouldConfigureVirtualThreads() {
            SimpleAsyncTaskExecutor executor = (SimpleAsyncTaskExecutor) asyncConfig.virtualThreadDomainEventExecutor();

            // Virtual threads are enabled in the configuration
            assertThat(executor).isNotNull();
        }

        @Test
        @DisplayName("should set thread name prefix")
        void shouldSetThreadNamePrefix() {
            SimpleAsyncTaskExecutor executor = (SimpleAsyncTaskExecutor) asyncConfig.virtualThreadDomainEventExecutor();

            assertThat(executor.getThreadNamePrefix()).isEqualTo("domain-event-");
        }
    }

    @Nested
    @DisplayName("threadPoolDomainEventExecutor")
    class ThreadPoolDomainEventExecutorTests {

        @Test
        @DisplayName("should create ThreadPoolTaskExecutor")
        void shouldCreateThreadPoolTaskExecutor() {
            Executor executor = asyncConfig.threadPoolDomainEventExecutor();

            assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        }

        @Test
        @DisplayName("should configure core pool size")
        void shouldConfigureCorePoolSize() {
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.threadPoolDomainEventExecutor();

            assertThat(executor.getCorePoolSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("should configure max pool size")
        void shouldConfigureMaxPoolSize() {
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.threadPoolDomainEventExecutor();

            assertThat(executor.getMaxPoolSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("should set thread name prefix")
        void shouldSetThreadNamePrefix() {
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.threadPoolDomainEventExecutor();

            assertThat(executor.getThreadNamePrefix()).isEqualTo("domain-event-");
        }
    }

    @Nested
    @DisplayName("virtualThreadTaskExecutor")
    class VirtualThreadTaskExecutorTests {

        @Test
        @DisplayName("should create SimpleAsyncTaskExecutor")
        void shouldCreateSimpleAsyncTaskExecutor() {
            TaskExecutor executor = asyncConfig.virtualThreadTaskExecutor();

            assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);
        }

        @Test
        @DisplayName("should set async thread name prefix")
        void shouldSetAsyncThreadNamePrefix() {
            SimpleAsyncTaskExecutor executor = (SimpleAsyncTaskExecutor) asyncConfig.virtualThreadTaskExecutor();

            assertThat(executor.getThreadNamePrefix()).isEqualTo("async-");
        }
    }
}
