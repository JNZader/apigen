/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jnzader.apigen.codegen.generator.rust;

import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import java.util.Map;

/**
 * Configuration options for Rust/Axum project generation.
 *
 * <p>Supports multiple presets for different deployment scenarios:
 *
 * <ul>
 *   <li>{@code cloud} - Cloud-native API with PostgreSQL, Redis, JWT, OpenTelemetry
 *   <li>{@code edge-gateway} - IoT Gateway with MQTT, Modbus, Serial support
 *   <li>{@code edge-anomaly} - Anomaly detection with SQLite, MQTT, ndarray
 *   <li>{@code edge-ai} - ML inference with ONNX Runtime, tokenizers
 * </ul>
 *
 * @author APiGen
 * @since 2.12.0
 */
public record RustAxumOptions(
        // Database
        boolean usePostgres,
        boolean useSqlite,

        // Cache/Session
        boolean useRedis,

        // Messaging
        boolean useMqtt,
        boolean useNats,

        // IoT/Edge
        boolean useModbus,
        boolean useSerial,

        // AI/ML
        boolean useOnnx,
        boolean useTokenizers,
        boolean useNdarray,

        // Auth/Security
        boolean useJwt,
        boolean useArgon2,

        // Observability
        boolean useTracing,
        boolean useOpenTelemetry,

        // Extras
        boolean useDocker,
        boolean useGracefulShutdown) {

    /** Cloud-native defaults: PostgreSQL + Redis + JWT + OpenTelemetry. */
    public static RustAxumOptions cloudDefaults() {
        return new RustAxumOptions(
                true, // usePostgres
                false, // useSqlite
                true, // useRedis
                false, // useMqtt
                false, // useNats
                false, // useModbus
                false, // useSerial
                false, // useOnnx
                false, // useTokenizers
                false, // useNdarray
                true, // useJwt
                true, // useArgon2
                true, // useTracing
                true, // useOpenTelemetry
                true, // useDocker
                true // useGracefulShutdown
                );
    }

    /** Edge gateway defaults: PostgreSQL/SQLite + MQTT + Modbus + Serial. */
    public static RustAxumOptions edgeGatewayDefaults() {
        return new RustAxumOptions(
                true, // usePostgres
                true, // useSqlite (fallback)
                false, // useRedis
                true, // useMqtt
                false, // useNats
                true, // useModbus
                true, // useSerial
                false, // useOnnx
                false, // useTokenizers
                false, // useNdarray
                true, // useJwt
                true, // useArgon2
                true, // useTracing
                true, // useOpenTelemetry
                true, // useDocker
                true // useGracefulShutdown
                );
    }

    /** Edge anomaly detection defaults: SQLite + MQTT consumer + ndarray. */
    public static RustAxumOptions edgeAnomalyDefaults() {
        return new RustAxumOptions(
                false, // usePostgres
                true, // useSqlite
                false, // useRedis
                true, // useMqtt
                false, // useNats
                false, // useModbus
                false, // useSerial
                false, // useOnnx
                false, // useTokenizers
                true, // useNdarray
                false, // useJwt
                false, // useArgon2
                true, // useTracing
                false, // useOpenTelemetry
                true, // useDocker
                true // useGracefulShutdown
                );
    }

    /** Edge AI defaults: PostgreSQL + ONNX Runtime + tokenizers. */
    public static RustAxumOptions edgeAiDefaults() {
        return new RustAxumOptions(
                true, // usePostgres
                false, // useSqlite
                true, // useRedis
                false, // useMqtt
                false, // useNats
                false, // useModbus
                false, // useSerial
                true, // useOnnx
                true, // useTokenizers
                true, // useNdarray
                true, // useJwt
                true, // useArgon2
                true, // useTracing
                true, // useOpenTelemetry
                true, // useDocker
                true // useGracefulShutdown
                );
    }

    /**
     * Creates options from a ProjectConfig, applying preset defaults and overrides.
     *
     * @param config the project configuration
     * @return configured options
     */
    public static RustAxumOptions fromConfig(ProjectConfig config) {
        Map<String, Object> options = config.getOptions();

        String preset = getStringOption(options, "preset", "cloud");
        RustAxumOptions base =
                switch (preset.toLowerCase()) {
                    case "edge-gateway" -> edgeGatewayDefaults();
                    case "edge-anomaly" -> edgeAnomalyDefaults();
                    case "edge-ai" -> edgeAiDefaults();
                    default -> cloudDefaults();
                };

        return new RustAxumOptions(
                getBoolOption(options, "usePostgres", base.usePostgres()),
                getBoolOption(options, "useSqlite", base.useSqlite()),
                getBoolOption(options, "useRedis", base.useRedis()),
                getBoolOption(options, "useMqtt", base.useMqtt()),
                getBoolOption(options, "useNats", base.useNats()),
                getBoolOption(options, "useModbus", base.useModbus()),
                getBoolOption(options, "useSerial", base.useSerial()),
                getBoolOption(options, "useOnnx", base.useOnnx()),
                getBoolOption(options, "useTokenizers", base.useTokenizers()),
                getBoolOption(options, "useNdarray", base.useNdarray()),
                getBoolOption(options, "useJwt", base.useJwt()),
                getBoolOption(options, "useArgon2", base.useArgon2()),
                getBoolOption(options, "useTracing", base.useTracing()),
                getBoolOption(options, "useOpenTelemetry", base.useOpenTelemetry()),
                getBoolOption(options, "useDocker", base.useDocker()),
                getBoolOption(options, "useGracefulShutdown", base.useGracefulShutdown()));
    }

    private static String getStringOption(Map<String, Object> options, String key, String defVal) {
        if (options == null) return defVal;
        Object value = options.get(key);
        return value instanceof String s ? s : defVal;
    }

    private static boolean getBoolOption(Map<String, Object> options, String key, boolean defVal) {
        if (options == null) return defVal;
        Object value = options.get(key);
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s);
        return defVal;
    }

    /** Returns true if any database is enabled. */
    public boolean hasDatabase() {
        return usePostgres || useSqlite;
    }

    /** Returns true if any messaging is enabled. */
    public boolean hasMessaging() {
        return useMqtt || useNats;
    }

    /** Returns true if any IoT/edge features are enabled. */
    public boolean hasEdgeFeatures() {
        return useModbus || useSerial || useMqtt;
    }

    /** Returns true if any AI/ML features are enabled. */
    public boolean hasAiFeatures() {
        return useOnnx || useTokenizers || useNdarray;
    }
}
