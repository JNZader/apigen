package com.jnzader.apigen.core.infrastructure.util;

import org.hibernate.stat.Statistics;

/**
 * Utility class for asserting SQL query counts in tests using Hibernate Statistics.
 *
 * <p>Provides convenient methods for detecting N+1 query problems.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @Autowired
 * private Statistics hibernateStatistics;
 *
 * @Test
 * void shouldNotHaveNPlusOneQueries() {
 *     QueryAssertions assertions = new QueryAssertions(hibernateStatistics);
 *     assertions.reset();
 *
 *     List<Product> products = productService.findAllWithCategory();
 *
 *     // Should only execute 1-2 queries (with JOIN FETCH)
 *     assertions.assertQueryCountLessOrEqual(2);
 * }
 * }</pre>
 */
public final class QueryAssertions {

    private final Statistics statistics;
    private long baselineQueryCount;
    private long baselineEntityLoadCount;

    /**
     * Creates a new QueryAssertions instance.
     *
     * @param statistics the Hibernate Statistics bean
     */
    public QueryAssertions(Statistics statistics) {
        this.statistics = statistics;
        reset();
    }

    /** Resets the baseline counts. Call this at the beginning of each test. */
    public void reset() {
        this.baselineQueryCount = statistics.getQueryExecutionCount();
        this.baselineEntityLoadCount = statistics.getEntityLoadCount();
    }

    /** Clears all statistics. Use with caution in parallel tests. */
    public void clearStatistics() {
        statistics.clear();
        reset();
    }

    /**
     * Gets the number of queries executed since the last reset.
     *
     * @return query count since reset
     */
    public long getQueryCount() {
        return statistics.getQueryExecutionCount() - baselineQueryCount;
    }

    /**
     * Gets the number of entities loaded since the last reset.
     *
     * @return entity load count since reset
     */
    public long getEntityLoadCount() {
        return statistics.getEntityLoadCount() - baselineEntityLoadCount;
    }

    /**
     * Asserts that exactly the specified number of queries were executed since reset.
     *
     * @param expected the expected number of queries
     * @throws AssertionError if the actual count doesn't match
     */
    public void assertQueryCount(long expected) {
        long actual = getQueryCount();
        if (actual != expected) {
            throw new AssertionError(
                    String.format(
                            "Expected %d queries but got %d. Consider using JOIN FETCH to avoid"
                                    + " N+1.",
                            expected, actual));
        }
    }

    /**
     * Asserts that the query count is less than or equal to the specified maximum.
     *
     * @param maxQueries the maximum number of queries allowed
     * @throws AssertionError if more queries were executed
     */
    public void assertQueryCountLessOrEqual(long maxQueries) {
        long actual = getQueryCount();
        if (actual > maxQueries) {
            throw new AssertionError(
                    String.format(
                            "Expected at most %d queries but got %d. Possible N+1 query problem.",
                            maxQueries, actual));
        }
    }

    /**
     * Asserts that the entity load count is less than or equal to the specified maximum.
     *
     * @param maxLoads the maximum number of entity loads allowed
     * @throws AssertionError if more entities were loaded
     */
    public void assertEntityLoadCountLessOrEqual(long maxLoads) {
        long actual = getEntityLoadCount();
        if (actual > maxLoads) {
            throw new AssertionError(
                    String.format(
                            "Expected at most %d entity loads but got %d. Check lazy loading"
                                    + " configuration.",
                            maxLoads, actual));
        }
    }

    /**
     * Gets a summary of all statistics for debugging.
     *
     * @return statistics summary string
     */
    public String getSummary() {
        return String.format(
                "Queries: %d, Entity Loads: %d, Collection Loads: %d, Cache Hits: %d",
                getQueryCount(),
                getEntityLoadCount(),
                statistics.getCollectionLoadCount(),
                statistics.getSecondLevelCacheHitCount());
    }
}
