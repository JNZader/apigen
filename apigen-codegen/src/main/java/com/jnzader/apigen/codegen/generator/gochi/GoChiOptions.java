package com.jnzader.apigen.codegen.generator.gochi;

import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import java.util.Map;

/**
 * Configuration options for Go/Chi project generation.
 *
 * <p>Supports various deployment scenarios:
 *
 * <ul>
 *   <li>Edge API: SQLite + MQTT + minimal footprint
 *   <li>Cloud API: PostgreSQL + Redis + NATS + multi-tenant
 * </ul>
 */
public record GoChiOptions(
        // Database
        boolean usePostgres,
        boolean useSqlite,
        boolean useTimescaleDb,
        boolean useMultiTenant,

        // Cache & Sessions
        boolean useRedis,

        // Messaging
        boolean useNats,
        boolean useMqtt,

        // Auth
        boolean useJwt,
        boolean useBcrypt,

        // Observability
        boolean useOpenTelemetry,

        // Database config
        String postgresSchema,
        boolean useRls // Row Level Security
        ) {

    /** Default options for a standard cloud API. */
    public static GoChiOptions cloudDefaults() {
        return new GoChiOptions(
                true, // postgres
                false, // sqlite
                false, // timescale
                false, // multi-tenant
                true, // redis
                false, // nats
                false, // mqtt
                true, // jwt
                true, // bcrypt
                true, // otel
                "public", false);
    }

    /** Default options for an edge API. */
    public static GoChiOptions edgeDefaults() {
        return new GoChiOptions(
                true, // postgres (timescale)
                true, // sqlite
                true, // timescale
                false, // multi-tenant
                false, // redis
                false, // nats
                true, // mqtt
                true, // jwt
                true, // bcrypt
                true, // otel
                "public", false);
    }

    /** Parse options from project config options. */
    public static GoChiOptions fromConfig(ProjectConfig config) {
        Map<String, Object> options = config.getOptions();
        if (options == null || options.isEmpty()) {
            return cloudDefaults();
        }

        // Check for preset
        String preset = getStringOption(options, "preset", "cloud");
        GoChiOptions base = "edge".equalsIgnoreCase(preset) ? edgeDefaults() : cloudDefaults();

        // Override with specific options
        return new GoChiOptions(
                getBoolOption(options, "postgres", base.usePostgres()),
                getBoolOption(options, "sqlite", base.useSqlite()),
                getBoolOption(options, "timescaledb", base.useTimescaleDb()),
                getBoolOption(options, "multiTenant", base.useMultiTenant()),
                getBoolOption(options, "redis", base.useRedis()),
                getBoolOption(options, "nats", base.useNats()),
                getBoolOption(options, "mqtt", base.useMqtt()),
                getBoolOption(options, "jwt", base.useJwt()),
                getBoolOption(options, "bcrypt", base.useBcrypt()),
                getBoolOption(options, "opentelemetry", base.useOpenTelemetry()),
                getStringOption(options, "postgresSchema", base.postgresSchema()),
                getBoolOption(options, "rls", base.useRls()));
    }

    private static boolean getBoolOption(Map<String, Object> extras, String key, boolean def) {
        Object value = extras.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return "true".equalsIgnoreCase(s);
        }
        return def;
    }

    private static String getStringOption(Map<String, Object> extras, String key, String def) {
        Object value = extras.get(key);
        if (value instanceof String s && !s.isBlank()) {
            return s;
        }
        return def;
    }
}
