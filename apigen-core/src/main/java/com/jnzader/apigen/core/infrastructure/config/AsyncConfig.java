package com.jnzader.apigen.core.infrastructure.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuración de procesamiento asíncrono.
 *
 * <p>Habilita @Async para event handlers y otras tareas asíncronas. Usa Virtual Threads cuando
 * están habilitados (Java 21+).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Executor para tareas de eventos de dominio usando Virtual Threads. Virtual Threads son más
     * eficientes para I/O-bound tasks.
     */
    @Bean(name = "domainEventExecutor")
    @ConditionalOnProperty(
            name = "spring.threads.virtual.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public Executor virtualThreadDomainEventExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("domain-event-");
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(
                task ->
                        () -> {
                            long start = System.currentTimeMillis();
                            try {
                                task.run();
                            } finally {
                                log.debug(
                                        "Domain event task completed in {}ms",
                                        System.currentTimeMillis() - start);
                            }
                        });

        log.info("Domain event executor initialized with Virtual Threads");
        return executor;
    }

    /** Executor alternativo usando ThreadPool para entornos sin Virtual Threads. */
    @Bean(name = "domainEventExecutor")
    @ConditionalOnProperty(name = "spring.threads.virtual.enabled", havingValue = "false")
    public Executor threadPoolDomainEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("domain-event-");
        executor.setRejectedExecutionHandler(
                (r, e) ->
                        log.warn(
                                "Domain event task rejected, queue full. Consider increasing"
                                        + " capacity."));
        executor.initialize();

        log.info(
                "Domain event executor initialized: corePoolSize={}, maxPoolSize={},"
                        + " queueCapacity={}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                100);

        return executor;
    }

    /** Executor general para tareas asíncronas con Virtual Threads. */
    @Bean(name = "taskExecutor")
    @ConditionalOnProperty(
            name = "spring.threads.virtual.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public TaskExecutor virtualThreadTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("async-");
        executor.setVirtualThreads(true);
        log.info("Default task executor configured with Virtual Threads");
        return executor;
    }
}
