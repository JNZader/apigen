package com.jnzader.apigen.core.support;

/**
 * Constants used across tests.
 */
public final class TestConstants {

    private TestConstants() {
        // Utility class
    }

    // IDs
    public static final Long VALID_ID = 1L;
    public static final Long INVALID_ID = 999L;

    // Names
    public static final String VALID_NAME = "Test Entity";
    public static final String UPDATED_NAME = "Updated Entity";
    public static final String INVALID_NAME = "";

    // Users
    public static final String TEST_USER = "test-user";
    public static final String ADMIN_USER = "admin";
    public static final String ANONYMOUS_USER = "anonymous";

    // Pagination
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    // Cache
    public static final String ENTITIES_CACHE = "entities";
    public static final String LISTS_CACHE = "lists";
    public static final String COUNTS_CACHE = "counts";

    // API Paths
    public static final String API_BASE_PATH = "/api";
    public static final String API_V1_PATH = "/api/v1";

    // Content Types
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_XML = "application/xml";

    // Test values
    public static final String TEST_DESCRIPTION = "Test Description";
    public static final Integer TEST_VALUE = 100;
}
