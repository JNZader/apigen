package com.jnzader.apigen.core.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for N+1 query detection and SQL query analysis.
 *
 * <p>Uses Hibernate Statistics to detect N+1 query problems and analyze query patterns during
 * development and testing.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Hibernate statistics for query counting
 *   <li>Query execution metrics
 *   <li>Slow query detection
 * </ul>
 *
 * <p>Enabled when: {@code apigen.query-analysis.enabled=true} (default: false - only enable in
 * dev/test)
 *
 * <p>Usage in tests:
 *
 * <pre>{@code
 * @Autowired
 * private Statistics hibernateStatistics;
 *
 * @Test
 * void testNoNPlusOne() {
 *     hibernateStatistics.clear();
 *
 *     // Perform operation
 *     service.findAll();
 *
 *     // Assert no N+1 queries occurred
 *     long queryCount = hibernateStatistics.getQueryExecutionCount();
 *     assertThat(queryCount).isLessThanOrEqualTo(2);
 * }
 * }</pre>
 */
@Configuration
@ConditionalOnClass(SessionFactory.class)
@ConditionalOnBean(EntityManagerFactory.class)
@ConditionalOnProperty(name = "apigen.query-analysis.enabled", havingValue = "true")
public class QueryAnalysisConfig {

    private static final Logger log = LoggerFactory.getLogger(QueryAnalysisConfig.class);

    private final EntityManagerFactory entityManagerFactory;

    public QueryAnalysisConfig(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Enables Hibernate statistics for query counting and analysis.
     *
     * @return the configured Statistics object
     */
    @Bean
    public Statistics hibernateStatistics() {
        SessionFactoryImplementor sessionFactory =
                entityManagerFactory.unwrap(SessionFactoryImplementor.class);

        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        log.info(
                "Hibernate statistics enabled for query analysis. Use hibernateStatistics bean"
                        + " to detect N+1 queries.");

        return statistics;
    }

    @PostConstruct
    public void logConfiguration() {
        log.info(
                "Query analysis enabled. Available metrics: queryExecutionCount,"
                        + " entityLoadCount, collectionLoadCount, secondLevelCacheHitCount");
    }
}
