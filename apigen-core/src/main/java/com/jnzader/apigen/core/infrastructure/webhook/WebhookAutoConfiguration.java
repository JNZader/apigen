package com.jnzader.apigen.core.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for webhook support.
 *
 * <p>Enabled by setting {@code apigen.webhooks.enabled=true} in application properties.
 *
 * <p>Configuration properties:
 *
 * <pre>
 * apigen.webhooks:
 *   enabled: true
 *   connect-timeout: 5s
 *   request-timeout: 30s
 *   max-retries: 3
 *   retry-base-delay: 1s
 *   retry-max-delay: 5m
 * </pre>
 */
@Configuration
@ConditionalOnProperty(name = "apigen.webhooks.enabled", havingValue = "true")
@EnableConfigurationProperties(WebhookAutoConfiguration.WebhookProperties.class)
public class WebhookAutoConfiguration {

    /**
     * Creates an in-memory subscription repository if no other is provided.
     *
     * @return the subscription repository
     */
    @Bean
    @ConditionalOnMissingBean(WebhookSubscriptionRepository.class)
    public WebhookSubscriptionRepository webhookSubscriptionRepository() {
        return new InMemoryWebhookSubscriptionRepository();
    }

    /**
     * Creates the webhook service.
     *
     * @param subscriptionRepository the subscription repository
     * @param objectMapper the object mapper
     * @param properties configuration properties
     * @return the webhook service
     */
    @Bean
    @ConditionalOnMissingBean(WebhookService.class)
    public WebhookService webhookService(
            WebhookSubscriptionRepository subscriptionRepository,
            ObjectMapper objectMapper,
            WebhookProperties properties) {
        WebhookService.WebhookConfig config =
                WebhookService.WebhookConfig.builder()
                        .connectTimeout(properties.getConnectTimeout())
                        .requestTimeout(properties.getRequestTimeout())
                        .maxRetries(properties.getMaxRetries())
                        .retryBaseDelay(properties.getRetryBaseDelay())
                        .retryMaxDelay(properties.getRetryMaxDelay())
                        .build();

        return new WebhookService(subscriptionRepository, objectMapper, config);
    }

    /** Configuration properties for webhooks. */
    @ConfigurationProperties(prefix = "apigen.webhooks")
    public static class WebhookProperties {

        /** Whether webhooks are enabled. */
        private boolean enabled = false;

        /** Connection timeout for webhook requests. */
        private Duration connectTimeout = Duration.ofSeconds(5);

        /** Request timeout for webhook requests. */
        private Duration requestTimeout = Duration.ofSeconds(30);

        /** Maximum number of retry attempts. */
        private int maxRetries = 3;

        /** Base delay for exponential backoff. */
        private Duration retryBaseDelay = Duration.ofSeconds(1);

        /** Maximum delay between retries. */
        private Duration retryMaxDelay = Duration.ofMinutes(5);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Duration getRetryBaseDelay() {
            return retryBaseDelay;
        }

        public void setRetryBaseDelay(Duration retryBaseDelay) {
            this.retryBaseDelay = retryBaseDelay;
        }

        public Duration getRetryMaxDelay() {
            return retryMaxDelay;
        }

        public void setRetryMaxDelay(Duration retryMaxDelay) {
            this.retryMaxDelay = retryMaxDelay;
        }
    }
}
