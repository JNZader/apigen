package com.jnzader.apigen.core.infrastructure.versioning;

/**
 * Thread-local holder for the current API version.
 *
 * <p>Provides access to the API version throughout the request lifecycle without needing to pass it
 * explicitly.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Service
 * public class ProductService {
 *
 *     public ProductDTO findById(Long id) {
 *         String version = VersionContext.getVersion();
 *         if ("1.0".equals(version)) {
 *             return findByIdV1(id);
 *         } else {
 *             return findByIdV2(id);
 *         }
 *     }
 * }
 * }</pre>
 */
public final class VersionContext {

    private static final ThreadLocal<String> CURRENT_VERSION = new ThreadLocal<>();

    private VersionContext() {
        // Utility class
    }

    /**
     * Sets the current API version.
     *
     * @param version the API version
     */
    public static void setVersion(String version) {
        CURRENT_VERSION.set(version);
    }

    /**
     * Gets the current API version.
     *
     * @return the API version, or null if not set
     */
    public static String getVersion() {
        return CURRENT_VERSION.get();
    }

    /**
     * Gets the current API version or a default.
     *
     * @param defaultVersion the default version if none is set
     * @return the API version
     */
    public static String getVersionOrDefault(String defaultVersion) {
        String version = CURRENT_VERSION.get();
        return version != null ? version : defaultVersion;
    }

    /**
     * Checks if the current version matches the expected version.
     *
     * @param expectedVersion the expected version
     * @return true if versions match
     */
    public static boolean isVersion(String expectedVersion) {
        String current = CURRENT_VERSION.get();
        if (current == null || expectedVersion == null) {
            return false;
        }
        // Exact match
        if (current.equals(expectedVersion)) {
            return true;
        }
        // Major version match (e.g., "1" matches "1.0")
        String[] currentParts = current.split("\\.");
        String[] expectedParts = expectedVersion.split("\\.");
        if (currentParts.length == 0 || expectedParts.length == 0) {
            return false;
        }
        return currentParts[0].equals(expectedParts[0]);
    }

    /**
     * Checks if the current version is at least the minimum version.
     *
     * @param minimumVersion the minimum version required
     * @return true if current version >= minimum version
     */
    public static boolean isAtLeast(String minimumVersion) {
        String current = CURRENT_VERSION.get();
        if (current == null) {
            return false;
        }
        return compareVersions(current, minimumVersion) >= 0;
    }

    /**
     * Compares two version strings.
     *
     * @param v1 first version
     * @param v2 second version
     * @return negative if v1 < v2, zero if equal, positive if v1 > v2
     */
    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    }

    private static int parseVersionPart(String part) {
        // Handle versions like "1.0-beta" by extracting numeric prefix
        StringBuilder numeric = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (Character.isDigit(c)) {
                numeric.append(c);
            } else {
                break;
            }
        }
        return numeric.isEmpty() ? 0 : Integer.parseInt(numeric.toString());
    }

    /** Clears the current version. Should be called at the end of request processing. */
    public static void clear() {
        CURRENT_VERSION.remove();
    }
}
