package com.jnzader.apigen.grpc.config;

import com.jnzader.apigen.grpc.client.GrpcChannelFactory;
import com.jnzader.apigen.grpc.health.HealthServiceManager;
import com.jnzader.apigen.grpc.interceptor.ExceptionHandlingInterceptor;
import com.jnzader.apigen.grpc.interceptor.LoggingClientInterceptor;
import com.jnzader.apigen.grpc.interceptor.LoggingServerInterceptor;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for APiGen gRPC support.
 *
 * <p>Provides default beans for gRPC server and client infrastructure.
 */
@AutoConfiguration
@EnableConfigurationProperties(GrpcProperties.class)
@ConditionalOnProperty(prefix = "apigen.grpc", name = "enabled", havingValue = "true")
public class GrpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "apigen.grpc.server",
            name = "logging.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public LoggingServerInterceptor loggingServerInterceptor() {
        return new LoggingServerInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExceptionHandlingInterceptor exceptionHandlingInterceptor() {
        return new ExceptionHandlingInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "apigen.grpc.client",
            name = "logging.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public LoggingClientInterceptor loggingClientInterceptor() {
        return new LoggingClientInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcChannelFactory grpcChannelFactory(List<ClientInterceptor> interceptors) {
        return new GrpcChannelFactory(interceptors);
    }

    @Bean
    @ConditionalOnMissingBean
    public HealthServiceManager healthServiceManager() {
        return new HealthServiceManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public List<ServerInterceptor> defaultServerInterceptors(
            ExceptionHandlingInterceptor exceptionHandler,
            LoggingServerInterceptor loggingInterceptor) {
        return List.of(exceptionHandler, loggingInterceptor);
    }
}
