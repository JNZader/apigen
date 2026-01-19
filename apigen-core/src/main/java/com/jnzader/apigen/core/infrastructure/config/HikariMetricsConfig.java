package com.jnzader.apigen.core.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for HikariCP connection pool metrics.
 *
 * <p>Exposes HikariCP metrics to Micrometer for monitoring via Prometheus/Grafana:
 *
 * <ul>
 *   <li>{@code hikaricp.connections} - Total connections in the pool
 *   <li>{@code hikaricp.connections.active} - Active connections
 *   <li>{@code hikaricp.connections.idle} - Idle connections
 *   <li>{@code hikaricp.connections.pending} - Threads waiting for a connection
 *   <li>{@code hikaricp.connections.timeout} - Connection timeout count
 *   <li>{@code hikaricp.connections.creation} - Connection creation time
 *   <li>{@code hikaricp.connections.acquire} - Connection acquisition time
 *   <li>{@code hikaricp.connections.usage} - Connection usage time
 * </ul>
 *
 * <p>Enabled when: {@code apigen.metrics.hikari.enabled=true} (default: true)
 *
 * <p>Configure Prometheus scraping:
 *
 * <pre>
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health,metrics,prometheus
 * </pre>
 */
@Configuration
@ConditionalOnClass({HikariDataSource.class, MeterRegistry.class})
@ConditionalOnBean({DataSource.class, MeterRegistry.class})
@ConditionalOnProperty(
        name = "apigen.metrics.hikari.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class HikariMetricsConfig {

    private static final Logger log = LoggerFactory.getLogger(HikariMetricsConfig.class);

    private final DataSource dataSource;
    private final MeterRegistry meterRegistry;

    public HikariMetricsConfig(DataSource dataSource, MeterRegistry meterRegistry) {
        this.dataSource = dataSource;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Registers HikariCP metrics with Micrometer after the bean is constructed.
     *
     * <p>HikariCP automatically registers metrics when setMetricRegistry is called.
     */
    @PostConstruct
    public void bindMetrics() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            hikariDataSource.setMetricRegistry(meterRegistry);

            log.info(
                    "HikariCP metrics registered with Micrometer. Pool name: {}, Max pool size: {},"
                            + " Min idle: {}",
                    hikariDataSource.getPoolName(),
                    hikariDataSource.getMaximumPoolSize(),
                    hikariDataSource.getMinimumIdle());
        } else {
            log.warn(
                    "DataSource is not HikariDataSource (found: {}). HikariCP metrics will not be"
                            + " registered.",
                    dataSource.getClass().getName());
        }
    }
}
