package com.jnzader.apigen.codegen.generator.gochi.config;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.gochi.GoChiOptions;
import com.jnzader.apigen.codegen.generator.gochi.GoChiTypeMapper;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Generates configuration files for Go/Chi projects using Viper. */
public class GoChiConfigGenerator {

    private final String moduleName;
    private final GoChiOptions options;
    private final GoChiTypeMapper typeMapper;

    public GoChiConfigGenerator(String moduleName, GoChiOptions options) {
        this.moduleName = moduleName;
        this.options = options;
        this.typeMapper = new GoChiTypeMapper();
    }

    public Map<String, String> generate(SqlSchema schema, ProjectConfig config) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("go.mod", generateGoMod());
        files.put("main.go", generateMainGo(schema));
        files.put("internal/config/config.go", generateConfigGo());
        files.put("config.yaml", generateConfigYaml());
        files.put("config.dev.yaml", generateConfigDevYaml());
        files.put(".env.example", generateEnvExample());
        files.put(".gitignore", generateGitignore());
        files.put("README.md", generateReadme(schema));
        files.put("Makefile", generateMakefile());

        if (config.isFeatureEnabled(Feature.DOCKER)) {
            files.put("Dockerfile", generateDockerfile());
            files.put("docker-compose.yml", generateDockerCompose());
            files.put(".dockerignore", generateDockerignore());
        }

        return files;
    }

    private String generateGoMod() {
        StringBuilder sb = new StringBuilder();
        sb.append("module ").append(moduleName).append("\n\n");
        sb.append("go 1.23\n\n");
        sb.append("require (\n");

        // Core
        sb.append("\tgithub.com/go-chi/chi/v5 v5.2.0\n");
        sb.append("\tgithub.com/go-chi/cors v1.2.1\n");
        sb.append("\tgithub.com/spf13/viper v1.19.0\n");
        sb.append("\tgithub.com/go-playground/validator/v10 v10.23.0\n");

        // Database
        if (options.usePostgres() || options.useTimescaleDb()) {
            sb.append("\tgithub.com/jackc/pgx/v5 v5.7.2\n");
        }
        if (options.useSqlite()) {
            sb.append("\tgithub.com/mattn/go-sqlite3 v1.14.24\n");
        }

        // Auth
        if (options.useJwt()) {
            sb.append("\tgithub.com/golang-jwt/jwt/v5 v5.2.1\n");
        }
        if (options.useBcrypt()) {
            sb.append("\tgolang.org/x/crypto v0.31.0\n");
        }

        // Cache
        if (options.useRedis()) {
            sb.append("\tgithub.com/redis/go-redis/v9 v9.7.0\n");
        }

        // Messaging
        if (options.useNats()) {
            sb.append("\tgithub.com/nats-io/nats.go v1.38.0\n");
        }
        if (options.useMqtt()) {
            sb.append("\tgithub.com/eclipse/paho.mqtt.golang v1.5.0\n");
        }

        // Observability
        if (options.useOpenTelemetry()) {
            sb.append("\tgo.opentelemetry.io/otel v1.33.0\n");
            sb.append("\tgo.opentelemetry.io/otel/exporters/otlp/otlptrace v1.33.0\n");
            sb.append("\tgo.opentelemetry.io/otel/sdk v1.33.0\n");
            sb.append("\tgo.opentelemetry.io/otel/trace v1.33.0\n");
        }

        // Utils
        sb.append("\tgithub.com/google/uuid v1.6.0\n");
        sb.append("\tgithub.com/shopspring/decimal v1.4.0\n");
        sb.append(")\n");

        return sb.toString();
    }

    private String generateMainGo(SqlSchema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("package main\n\n");

        sb.append("import (\n");
        sb.append("\t\"context\"\n");
        sb.append("\t\"log/slog\"\n");
        sb.append("\t\"net/http\"\n");
        sb.append("\t\"os\"\n");
        sb.append("\t\"os/signal\"\n");
        sb.append("\t\"syscall\"\n");
        sb.append("\t\"time\"\n");
        sb.append("\n");
        sb.append("\t\"").append(moduleName).append("/internal/config\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/database\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/handler\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/repository\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/router\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/service\"\n");
        if (options.useRedis()) {
            sb.append("\t\"").append(moduleName).append("/internal/cache\"\n");
        }
        if (options.useNats() || options.useMqtt()) {
            sb.append("\t\"").append(moduleName).append("/internal/messaging\"\n");
        }
        if (options.useOpenTelemetry()) {
            sb.append("\t\"").append(moduleName).append("/internal/telemetry\"\n");
        }
        sb.append(")\n\n");

        sb.append("func main() {\n");
        sb.append("\t// Setup structured logging\n");
        sb.append(
                "\tlogger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level:"
                        + " slog.LevelInfo}))\n");
        sb.append("\tslog.SetDefault(logger)\n\n");

        sb.append("\t// Load configuration\n");
        sb.append("\tcfg, err := config.Load()\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tslog.Error(\"failed to load config\", \"error\", err)\n");
        sb.append("\t\tos.Exit(1)\n");
        sb.append("\t}\n\n");

        if (options.useOpenTelemetry()) {
            sb.append("\t// Initialize OpenTelemetry\n");
            sb.append("\tshutdownTracer, err := telemetry.InitTracer(cfg)\n");
            sb.append("\tif err != nil {\n");
            sb.append("\t\tslog.Error(\"failed to init tracer\", \"error\", err)\n");
            sb.append("\t\tos.Exit(1)\n");
            sb.append("\t}\n");
            sb.append("\tdefer shutdownTracer(context.Background())\n\n");
        }

        sb.append("\t// Initialize database\n");
        sb.append("\tdb, err := database.NewPostgres(cfg)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tslog.Error(\"failed to connect to database\", \"error\", err)\n");
        sb.append("\t\tos.Exit(1)\n");
        sb.append("\t}\n");
        sb.append("\tdefer db.Close()\n\n");

        sb.append("\t// Run migrations\n");
        sb.append("\tif err := database.RunMigrations(db); err != nil {\n");
        sb.append("\t\tslog.Error(\"failed to run migrations\", \"error\", err)\n");
        sb.append("\t\tos.Exit(1)\n");
        sb.append("\t}\n\n");

        if (options.useRedis()) {
            sb.append("\t// Initialize Redis\n");
            sb.append("\tredisClient, err := cache.NewRedis(cfg)\n");
            sb.append("\tif err != nil {\n");
            sb.append("\t\tslog.Error(\"failed to connect to redis\", \"error\", err)\n");
            sb.append("\t\tos.Exit(1)\n");
            sb.append("\t}\n");
            sb.append("\tdefer redisClient.Close()\n\n");
        }

        if (options.useNats()) {
            sb.append("\t// Initialize NATS\n");
            sb.append("\tnatsConn, err := messaging.NewNats(cfg)\n");
            sb.append("\tif err != nil {\n");
            sb.append("\t\tslog.Error(\"failed to connect to nats\", \"error\", err)\n");
            sb.append("\t\tos.Exit(1)\n");
            sb.append("\t}\n");
            sb.append("\tdefer natsConn.Close()\n\n");
        }

        if (options.useMqtt()) {
            sb.append("\t// Initialize MQTT\n");
            sb.append("\tmqttClient, err := messaging.NewMqtt(cfg)\n");
            sb.append("\tif err != nil {\n");
            sb.append("\t\tslog.Error(\"failed to connect to mqtt\", \"error\", err)\n");
            sb.append("\t\tos.Exit(1)\n");
            sb.append("\t}\n");
            sb.append("\tdefer mqttClient.Disconnect(250)\n\n");
        }

        sb.append("\t// Initialize repositories\n");
        for (SqlTable table : schema.getEntityTables()) {
            String name = typeMapper.toExportedName(table.getEntityName());
            String varName = typeMapper.toUnexportedName(name) + "Repo";
            sb.append("\t")
                    .append(varName)
                    .append(" := repository.New")
                    .append(name)
                    .append("Repository(db)\n");
        }
        sb.append("\n");

        sb.append("\t// Initialize services\n");
        for (SqlTable table : schema.getEntityTables()) {
            String name = typeMapper.toExportedName(table.getEntityName());
            String repoVar = typeMapper.toUnexportedName(name) + "Repo";
            String svcVar = typeMapper.toUnexportedName(name) + "Svc";
            sb.append("\t")
                    .append(svcVar)
                    .append(" := service.New")
                    .append(name)
                    .append("Service(")
                    .append(repoVar)
                    .append(")\n");
        }
        sb.append("\n");

        sb.append("\t// Initialize handlers\n");
        sb.append("\thandlers := &router.Handlers{\n");
        for (SqlTable table : schema.getEntityTables()) {
            String name = typeMapper.toExportedName(table.getEntityName());
            String svcVar = typeMapper.toUnexportedName(name) + "Svc";
            sb.append("\t\t")
                    .append(name)
                    .append(": handler.New")
                    .append(name)
                    .append("Handler(")
                    .append(svcVar)
                    .append("),\n");
        }
        sb.append("\t}\n\n");

        sb.append("\t// Setup router\n");
        sb.append("\tr := router.New(cfg, handlers)\n\n");

        sb.append("\t// Create server\n");
        sb.append("\tsrv := &http.Server{\n");
        sb.append("\t\tAddr:         \":\" + cfg.Server.Port,\n");
        sb.append("\t\tHandler:      r,\n");
        sb.append("\t\tReadTimeout:  15 * time.Second,\n");
        sb.append("\t\tWriteTimeout: 15 * time.Second,\n");
        sb.append("\t\tIdleTimeout:  60 * time.Second,\n");
        sb.append("\t}\n\n");

        sb.append("\t// Graceful shutdown\n");
        sb.append("\tgo func() {\n");
        sb.append(
                "\t\tslog.Info(\"server starting\", \"port\", cfg.Server.Port, \"env\","
                        + " cfg.Server.Environment)\n");
        sb.append(
                "\t\tif err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed"
                        + " {\n");
        sb.append("\t\t\tslog.Error(\"server error\", \"error\", err)\n");
        sb.append("\t\t\tos.Exit(1)\n");
        sb.append("\t\t}\n");
        sb.append("\t}()\n\n");

        sb.append("\t// Wait for interrupt signal\n");
        sb.append("\tquit := make(chan os.Signal, 1)\n");
        sb.append("\tsignal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)\n");
        sb.append("\t<-quit\n\n");

        sb.append("\tslog.Info(\"shutting down server...\")\n");
        sb.append("\tctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)\n");
        sb.append("\tdefer cancel()\n\n");

        sb.append("\tif err := srv.Shutdown(ctx); err != nil {\n");
        sb.append("\t\tslog.Error(\"server forced to shutdown\", \"error\", err)\n");
        sb.append("\t}\n\n");

        sb.append("\tslog.Info(\"server stopped\")\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String generateConfigGo() {
        StringBuilder sb = new StringBuilder();
        sb.append("package config\n\n");

        sb.append("import (\n");
        sb.append("\t\"fmt\"\n");
        sb.append("\t\"strings\"\n\n");
        sb.append("\t\"github.com/spf13/viper\"\n");
        sb.append(")\n\n");

        sb.append("// Config holds all application configuration.\n");
        sb.append("type Config struct {\n");
        sb.append("\tServer   ServerConfig\n");
        sb.append("\tDatabase DatabaseConfig\n");
        if (options.useRedis()) {
            sb.append("\tRedis    RedisConfig\n");
        }
        if (options.useNats()) {
            sb.append("\tNats     NatsConfig\n");
        }
        if (options.useMqtt()) {
            sb.append("\tMqtt     MqttConfig\n");
        }
        if (options.useJwt()) {
            sb.append("\tJWT      JWTConfig\n");
        }
        if (options.useOpenTelemetry()) {
            sb.append("\tTelemetry TelemetryConfig\n");
        }
        sb.append("}\n\n");

        sb.append("// ServerConfig holds HTTP server configuration.\n");
        sb.append("type ServerConfig struct {\n");
        sb.append("\tPort        string `mapstructure:\"port\"`\n");
        sb.append("\tEnvironment string `mapstructure:\"environment\"`\n");
        sb.append("\tDebug       bool   `mapstructure:\"debug\"`\n");
        sb.append("}\n\n");

        sb.append("// DatabaseConfig holds database configuration.\n");
        sb.append("type DatabaseConfig struct {\n");
        sb.append("\tHost         string `mapstructure:\"host\"`\n");
        sb.append("\tPort         int    `mapstructure:\"port\"`\n");
        sb.append("\tUser         string `mapstructure:\"user\"`\n");
        sb.append("\tPassword     string `mapstructure:\"password\"`\n");
        sb.append("\tName         string `mapstructure:\"name\"`\n");
        sb.append("\tSSLMode      string `mapstructure:\"sslmode\"`\n");
        sb.append("\tMaxOpenConns int    `mapstructure:\"max_open_conns\"`\n");
        sb.append("\tMaxIdleConns int    `mapstructure:\"max_idle_conns\"`\n");
        if (options.useMultiTenant()) {
            sb.append("\tSchema       string `mapstructure:\"schema\"`\n");
        }
        sb.append("}\n\n");

        if (options.useRedis()) {
            sb.append("// RedisConfig holds Redis configuration.\n");
            sb.append("type RedisConfig struct {\n");
            sb.append("\tHost     string `mapstructure:\"host\"`\n");
            sb.append("\tPort     int    `mapstructure:\"port\"`\n");
            sb.append("\tPassword string `mapstructure:\"password\"`\n");
            sb.append("\tDB       int    `mapstructure:\"db\"`\n");
            sb.append("}\n\n");
        }

        if (options.useNats()) {
            sb.append("// NatsConfig holds NATS configuration.\n");
            sb.append("type NatsConfig struct {\n");
            sb.append("\tURL string `mapstructure:\"url\"`\n");
            sb.append("}\n\n");
        }

        if (options.useMqtt()) {
            sb.append("// MqttConfig holds MQTT configuration.\n");
            sb.append("type MqttConfig struct {\n");
            sb.append("\tBroker   string `mapstructure:\"broker\"`\n");
            sb.append("\tClientID string `mapstructure:\"client_id\"`\n");
            sb.append("\tUsername string `mapstructure:\"username\"`\n");
            sb.append("\tPassword string `mapstructure:\"password\"`\n");
            sb.append("}\n\n");
        }

        if (options.useJwt()) {
            sb.append("// JWTConfig holds JWT configuration.\n");
            sb.append("type JWTConfig struct {\n");
            sb.append("\tSecret          string `mapstructure:\"secret\"`\n");
            sb.append("\tExpirationHours int    `mapstructure:\"expiration_hours\"`\n");
            sb.append("\tIssuer          string `mapstructure:\"issuer\"`\n");
            sb.append("}\n\n");
        }

        if (options.useOpenTelemetry()) {
            sb.append("// TelemetryConfig holds OpenTelemetry configuration.\n");
            sb.append("type TelemetryConfig struct {\n");
            sb.append("\tEnabled     bool   `mapstructure:\"enabled\"`\n");
            sb.append("\tServiceName string `mapstructure:\"service_name\"`\n");
            sb.append("\tEndpoint    string `mapstructure:\"endpoint\"`\n");
            sb.append("}\n\n");
        }

        sb.append("// Load reads configuration from file and environment.\n");
        sb.append("func Load() (*Config, error) {\n");
        sb.append("\tv := viper.New()\n\n");

        sb.append("\t// Config file settings\n");
        sb.append("\tv.SetConfigName(\"config\")\n");
        sb.append("\tv.SetConfigType(\"yaml\")\n");
        sb.append("\tv.AddConfigPath(\".\")\n");
        sb.append("\tv.AddConfigPath(\"./config\")\n");
        sb.append("\tv.AddConfigPath(\"/etc/app\")\n\n");

        sb.append("\t// Environment variables\n");
        sb.append("\tv.AutomaticEnv()\n");
        sb.append("\tv.SetEnvKeyReplacer(strings.NewReplacer(\".\", \"_\"))\n\n");

        sb.append("\t// Defaults\n");
        sb.append("\tv.SetDefault(\"server.port\", \"8080\")\n");
        sb.append("\tv.SetDefault(\"server.environment\", \"development\")\n");
        sb.append("\tv.SetDefault(\"database.host\", \"localhost\")\n");
        sb.append("\tv.SetDefault(\"database.port\", 5432)\n");
        sb.append("\tv.SetDefault(\"database.sslmode\", \"disable\")\n");
        sb.append("\tv.SetDefault(\"database.max_open_conns\", 25)\n");
        sb.append("\tv.SetDefault(\"database.max_idle_conns\", 5)\n");
        if (options.useRedis()) {
            sb.append("\tv.SetDefault(\"redis.host\", \"localhost\")\n");
            sb.append("\tv.SetDefault(\"redis.port\", 6379)\n");
            sb.append("\tv.SetDefault(\"redis.db\", 0)\n");
        }
        if (options.useJwt()) {
            sb.append("\tv.SetDefault(\"jwt.expiration_hours\", 24)\n");
            sb.append("\tv.SetDefault(\"jwt.issuer\", \"").append(getProjectName()).append("\")\n");
        }
        if (options.useOpenTelemetry()) {
            sb.append("\tv.SetDefault(\"telemetry.enabled\", true)\n");
            sb.append("\tv.SetDefault(\"telemetry.service_name\", \"")
                    .append(getProjectName())
                    .append("\")\n");
        }
        sb.append("\n");

        sb.append("\t// Read config file (optional)\n");
        sb.append("\tif err := v.ReadInConfig(); err != nil {\n");
        sb.append("\t\tif _, ok := err.(viper.ConfigFileNotFoundError); !ok {\n");
        sb.append("\t\t\treturn nil, fmt.Errorf(\"error reading config: %w\", err)\n");
        sb.append("\t\t}\n");
        sb.append("\t}\n\n");

        sb.append("\t// Check for environment-specific config\n");
        sb.append("\tenv := v.GetString(\"server.environment\")\n");
        sb.append("\tv.SetConfigName(\"config.\" + env)\n");
        sb.append("\t_ = v.MergeInConfig()\n\n");

        sb.append("\tvar cfg Config\n");
        sb.append("\tif err := v.Unmarshal(&cfg); err != nil {\n");
        sb.append("\t\treturn nil, fmt.Errorf(\"error unmarshaling config: %w\", err)\n");
        sb.append("\t}\n\n");

        sb.append("\treturn &cfg, nil\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String generateConfigYaml() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Application configuration\n\n");

        sb.append("server:\n");
        sb.append("  port: \"8080\"\n");
        sb.append("  environment: \"production\"\n");
        sb.append("  debug: false\n\n");

        sb.append("database:\n");
        sb.append("  host: \"${DB_HOST:localhost}\"\n");
        sb.append("  port: ${DB_PORT:5432}\n");
        sb.append("  user: \"${DB_USER:postgres}\"\n");
        sb.append("  password: \"${DB_PASSWORD}\"\n");
        sb.append("  name: \"${DB_NAME:").append(toSnakeCase(getProjectName())).append("}\"\n");
        sb.append("  sslmode: \"${DB_SSLMODE:require}\"\n");
        sb.append("  max_open_conns: 25\n");
        sb.append("  max_idle_conns: 5\n");
        if (options.useMultiTenant()) {
            sb.append("  schema: \"${DB_SCHEMA:public}\"\n");
        }
        sb.append("\n");

        if (options.useRedis()) {
            sb.append("redis:\n");
            sb.append("  host: \"${REDIS_HOST:localhost}\"\n");
            sb.append("  port: ${REDIS_PORT:6379}\n");
            sb.append("  password: \"${REDIS_PASSWORD}\"\n");
            sb.append("  db: 0\n\n");
        }

        if (options.useNats()) {
            sb.append("nats:\n");
            sb.append("  url: \"${NATS_URL:nats://localhost:4222}\"\n\n");
        }

        if (options.useMqtt()) {
            sb.append("mqtt:\n");
            sb.append("  broker: \"${MQTT_BROKER:tcp://localhost:1883}\"\n");
            sb.append("  client_id: \"${MQTT_CLIENT_ID:").append(getProjectName()).append("}\"\n");
            sb.append("  username: \"${MQTT_USERNAME}\"\n");
            sb.append("  password: \"${MQTT_PASSWORD}\"\n\n");
        }

        if (options.useJwt()) {
            sb.append("jwt:\n");
            sb.append("  secret: \"${JWT_SECRET}\"\n");
            sb.append("  expiration_hours: 24\n");
            sb.append("  issuer: \"").append(getProjectName()).append("\"\n\n");
        }

        if (options.useOpenTelemetry()) {
            sb.append("telemetry:\n");
            sb.append("  enabled: true\n");
            sb.append("  service_name: \"").append(getProjectName()).append("\"\n");
            sb.append("  endpoint: \"${OTEL_ENDPOINT:localhost:4317}\"\n");
        }

        return sb.toString();
    }

    private String generateConfigDevYaml() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Development configuration overrides\n\n");

        sb.append("server:\n");
        sb.append("  environment: \"development\"\n");
        sb.append("  debug: true\n\n");

        sb.append("database:\n");
        sb.append("  host: \"localhost\"\n");
        sb.append("  sslmode: \"disable\"\n");

        if (options.useOpenTelemetry()) {
            sb.append("\ntelemetry:\n");
            sb.append("  enabled: false\n");
        }

        return sb.toString();
    }

    private String generateEnvExample() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Server\n");
        sb.append("PORT=8080\n");
        sb.append("SERVER_ENVIRONMENT=development\n\n");

        sb.append("# Database\n");
        sb.append("DB_HOST=localhost\n");
        sb.append("DB_PORT=5432\n");
        sb.append("DB_USER=postgres\n");
        sb.append("DB_PASSWORD=postgres\n");
        sb.append("DB_NAME=").append(toSnakeCase(getProjectName())).append("\n");
        sb.append("DB_SSLMODE=disable\n");
        if (options.useMultiTenant()) {
            sb.append("DB_SCHEMA=public\n");
        }
        sb.append("\n");

        if (options.useRedis()) {
            sb.append("# Redis\n");
            sb.append("REDIS_HOST=localhost\n");
            sb.append("REDIS_PORT=6379\n");
            sb.append("REDIS_PASSWORD=\n\n");
        }

        if (options.useNats()) {
            sb.append("# NATS\n");
            sb.append("NATS_URL=nats://localhost:4222\n\n");
        }

        if (options.useMqtt()) {
            sb.append("# MQTT\n");
            sb.append("MQTT_BROKER=tcp://localhost:1883\n");
            sb.append("MQTT_CLIENT_ID=").append(getProjectName()).append("\n");
            sb.append("MQTT_USERNAME=\n");
            sb.append("MQTT_PASSWORD=\n\n");
        }

        if (options.useJwt()) {
            sb.append("# JWT\n");
            sb.append("JWT_SECRET=your-super-secret-key-change-in-production\n\n");
        }

        if (options.useOpenTelemetry()) {
            sb.append("# OpenTelemetry\n");
            sb.append("OTEL_ENDPOINT=localhost:4317\n");
        }

        return sb.toString();
    }

    private String generateGitignore() {
        return """
        # Binaries
        *.exe
        *.exe~
        *.dll
        *.so
        *.dylib
        /bin/
        /build/

        # Test
        *.test
        *.out
        coverage.html

        # Config
        config.local.yaml
        .env
        .env.local
        .env.*.local

        # IDE
        .idea/
        .vscode/
        *.swp
        *.swo

        # OS
        .DS_Store
        Thumbs.db

        # Vendor
        /vendor/

        # Temp
        /tmp/
        """;
    }

    private String generateReadme(SqlSchema schema) {
        StringBuilder sb = new StringBuilder();
        String name = getProjectName();

        sb.append("# ").append(name).append("\n\n");
        sb.append("REST API built with Go, Chi Router, and pgx.\n\n");

        sb.append("## Tech Stack\n\n");
        sb.append("- **Router**: Chi v5\n");
        sb.append("- **Database**: PostgreSQL with pgx (no ORM)\n");
        sb.append("- **Config**: Viper\n");
        if (options.useJwt()) {
            sb.append("- **Auth**: JWT + bcrypt\n");
        }
        if (options.useRedis()) {
            sb.append("- **Cache**: Redis\n");
        }
        if (options.useNats()) {
            sb.append("- **Messaging**: NATS\n");
        }
        if (options.useMqtt()) {
            sb.append("- **Messaging**: MQTT\n");
        }
        if (options.useOpenTelemetry()) {
            sb.append("- **Observability**: OpenTelemetry\n");
        }
        sb.append("\n");

        sb.append("## Quick Start\n\n");
        sb.append("```bash\n");
        sb.append("# Configure\n");
        sb.append("cp .env.example .env\n");
        sb.append("# Edit .env with your settings\n\n");
        sb.append("# Run\n");
        sb.append("make run\n");
        sb.append("```\n\n");

        sb.append("## API Endpoints\n\n");
        for (SqlTable table : schema.getEntityTables()) {
            String entity = table.getEntityName();
            String path = typeMapper.toSnakeCase(typeMapper.pluralize(entity));
            sb.append("### ").append(entity).append("\n\n");
            sb.append("| Method | Endpoint | Description |\n");
            sb.append("|--------|----------|-------------|\n");
            sb.append("| GET | `/api/v1/").append(path).append("` | List all |\n");
            sb.append("| GET | `/api/v1/").append(path).append("/{id}` | Get by ID |\n");
            sb.append("| POST | `/api/v1/").append(path).append("` | Create |\n");
            sb.append("| PUT | `/api/v1/").append(path).append("/{id}` | Update |\n");
            sb.append("| DELETE | `/api/v1/").append(path).append("/{id}` | Delete |\n\n");
        }

        sb.append("## Make Commands\n\n");
        sb.append("```bash\n");
        sb.append("make run       # Run the application\n");
        sb.append("make build     # Build binary\n");
        sb.append("make test      # Run tests\n");
        sb.append("make lint      # Run linter\n");
        sb.append("make migrate   # Run migrations\n");
        sb.append("```\n");

        return sb.toString();
    }

    private String generateMakefile() {
        String binary = toSnakeCase(getProjectName());
        return """
        .PHONY: run build test lint clean migrate

        BINARY=%s

        run:
        	go run main.go

        build:
        	CGO_ENABLED=0 go build -o bin/$(BINARY) main.go

        test:
        	go test -v -race -coverprofile=coverage.out ./...

        coverage: test
        	go tool cover -html=coverage.out -o coverage.html

        lint:
        	golangci-lint run ./...

        clean:
        	rm -rf bin/ coverage.out coverage.html

        tidy:
        	go mod tidy

        migrate:
        	go run cmd/migrate/main.go

        docker-build:
        	docker build -t $(BINARY) .

        docker-up:
        	docker-compose up -d

        docker-down:
        	docker-compose down
        """
                .formatted(binary);
    }

    private String generateDockerfile() {
        String binary = toSnakeCase(getProjectName());
        return """
        FROM golang:1.23-alpine AS builder
        WORKDIR /app
        RUN apk add --no-cache git
        COPY go.mod go.sum ./
        RUN go mod download
        COPY . .
        RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o bin/%s main.go

        FROM alpine:3.21
        WORKDIR /app
        RUN apk --no-cache add ca-certificates tzdata
        COPY --from=builder /app/bin/%s .
        COPY --from=builder /app/config.yaml .
        RUN adduser -D -g '' appuser
        USER appuser
        EXPOSE 8080
        ENTRYPOINT ["./%s"]
        """
                .formatted(binary, binary, binary);
    }

    private String generateDockerCompose() {
        String name = toSnakeCase(getProjectName());
        StringBuilder sb = new StringBuilder();

        sb.append("services:\n");
        sb.append("  app:\n");
        sb.append("    build: .\n");
        sb.append("    container_name: ").append(name).append("-api\n");
        sb.append("    ports:\n");
        sb.append("      - \"8080:8080\"\n");
        sb.append("    environment:\n");
        sb.append("      - SERVER_ENVIRONMENT=production\n");
        sb.append("      - DB_HOST=db\n");
        sb.append("      - DB_PORT=5432\n");
        sb.append("      - DB_USER=postgres\n");
        sb.append("      - DB_PASSWORD=postgres\n");
        sb.append("      - DB_NAME=").append(name).append("\n");
        if (options.useRedis()) {
            sb.append("      - REDIS_HOST=redis\n");
        }
        if (options.useNats()) {
            sb.append("      - NATS_URL=nats://nats:4222\n");
        }
        sb.append("    depends_on:\n");
        sb.append("      db:\n");
        sb.append("        condition: service_healthy\n");
        if (options.useRedis()) {
            sb.append("      redis:\n");
            sb.append("        condition: service_healthy\n");
        }
        sb.append("    restart: unless-stopped\n\n");

        sb.append("  db:\n");
        if (options.useTimescaleDb()) {
            sb.append("    image: timescale/timescaledb:latest-pg16\n");
        } else {
            sb.append("    image: postgres:16-alpine\n");
        }
        sb.append("    container_name: ").append(name).append("-db\n");
        sb.append("    environment:\n");
        sb.append("      - POSTGRES_USER=postgres\n");
        sb.append("      - POSTGRES_PASSWORD=postgres\n");
        sb.append("      - POSTGRES_DB=").append(name).append("\n");
        sb.append("    volumes:\n");
        sb.append("      - postgres_data:/var/lib/postgresql/data\n");
        sb.append("    ports:\n");
        sb.append("      - \"5432:5432\"\n");
        sb.append("    healthcheck:\n");
        sb.append("      test: [\"CMD-SHELL\", \"pg_isready -U postgres\"]\n");
        sb.append("      interval: 10s\n");
        sb.append("      timeout: 5s\n");
        sb.append("      retries: 5\n");
        sb.append("    restart: unless-stopped\n");

        if (options.useRedis()) {
            sb.append("\n  redis:\n");
            sb.append("    image: redis:7-alpine\n");
            sb.append("    container_name: ").append(name).append("-redis\n");
            sb.append("    ports:\n");
            sb.append("      - \"6379:6379\"\n");
            sb.append("    healthcheck:\n");
            sb.append("      test: [\"CMD\", \"redis-cli\", \"ping\"]\n");
            sb.append("      interval: 10s\n");
            sb.append("      timeout: 5s\n");
            sb.append("      retries: 5\n");
            sb.append("    restart: unless-stopped\n");
        }

        if (options.useNats()) {
            sb.append("\n  nats:\n");
            sb.append("    image: nats:2-alpine\n");
            sb.append("    container_name: ").append(name).append("-nats\n");
            sb.append("    ports:\n");
            sb.append("      - \"4222:4222\"\n");
            sb.append("    restart: unless-stopped\n");
        }

        sb.append("\nvolumes:\n");
        sb.append("  postgres_data:\n");

        return sb.toString();
    }

    private String generateDockerignore() {
        return """
        bin/
        tmp/
        *.exe
        *.test
        *.out
        .git
        .gitignore
        .env
        *.md
        .idea
        .vscode
        coverage.*
        """;
    }

    public String generateRedis() {
        return """
        package cache

        import (
        	"context"
        	"fmt"

        	"%s/internal/config"
        	"github.com/redis/go-redis/v9"
        )

        // NewRedis creates a new Redis client.
        func NewRedis(cfg *config.Config) (*redis.Client, error) {
        	client := redis.NewClient(&redis.Options{
        		Addr:     fmt.Sprintf("%%s:%%d", cfg.Redis.Host, cfg.Redis.Port),
        		Password: cfg.Redis.Password,
        		DB:       cfg.Redis.DB,
        	})

        	ctx := context.Background()
        	if err := client.Ping(ctx).Err(); err != nil {
        		return nil, fmt.Errorf("failed to ping redis: %%w", err)
        	}

        	return client, nil
        }
        """
                .formatted(moduleName);
    }

    public String generateNats() {
        return """
        package messaging

        import (
        	"fmt"

        	"%s/internal/config"
        	"github.com/nats-io/nats.go"
        )

        // NewNats creates a new NATS connection.
        func NewNats(cfg *config.Config) (*nats.Conn, error) {
        	nc, err := nats.Connect(cfg.Nats.URL)
        	if err != nil {
        		return nil, fmt.Errorf("failed to connect to nats: %%w", err)
        	}
        	return nc, nil
        }
        """
                .formatted(moduleName);
    }

    public String generateMqtt() {
        return """
        package messaging

        import (
        	"fmt"
        	"time"

        	"%s/internal/config"
        	mqtt "github.com/eclipse/paho.mqtt.golang"
        )

        // NewMqtt creates a new MQTT client.
        func NewMqtt(cfg *config.Config) (mqtt.Client, error) {
        	opts := mqtt.NewClientOptions().
        		AddBroker(cfg.Mqtt.Broker).
        		SetClientID(cfg.Mqtt.ClientID).
        		SetUsername(cfg.Mqtt.Username).
        		SetPassword(cfg.Mqtt.Password).
        		SetAutoReconnect(true).
        		SetConnectRetry(true).
        		SetConnectRetryInterval(5 * time.Second)

        	client := mqtt.NewClient(opts)
        	token := client.Connect()
        	if token.Wait() && token.Error() != nil {
        		return nil, fmt.Errorf("failed to connect to mqtt: %%w", token.Error())
        	}

        	return client, nil
        }
        """
                .formatted(moduleName);
    }

    public String generateOpenTelemetry() {
        return """
        package telemetry

        import (
        	"context"
        	"fmt"

        	"%s/internal/config"
        	"go.opentelemetry.io/otel"
        	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
        	"go.opentelemetry.io/otel/propagation"
        	"go.opentelemetry.io/otel/sdk/resource"
        	sdktrace "go.opentelemetry.io/otel/sdk/trace"
        	semconv "go.opentelemetry.io/otel/semconv/v1.24.0"
        )

        // InitTracer initializes OpenTelemetry tracing.
        func InitTracer(cfg *config.Config) (func(context.Context) error, error) {
        	if !cfg.Telemetry.Enabled {
        		return func(context.Context) error { return nil }, nil
        	}

        	ctx := context.Background()

        	exporter, err := otlptracegrpc.New(ctx,
        		otlptracegrpc.WithEndpoint(cfg.Telemetry.Endpoint),
        		otlptracegrpc.WithInsecure(),
        	)
        	if err != nil {
        		return nil, fmt.Errorf("failed to create exporter: %%w", err)
        	}

        	res, err := resource.New(ctx,
        		resource.WithAttributes(
        			semconv.ServiceNameKey.String(cfg.Telemetry.ServiceName),
        		),
        	)
        	if err != nil {
        		return nil, fmt.Errorf("failed to create resource: %%w", err)
        	}

        	tp := sdktrace.NewTracerProvider(
        		sdktrace.WithBatcher(exporter),
        		sdktrace.WithResource(res),
        	)

        	otel.SetTracerProvider(tp)
        	otel.SetTextMapPropagator(propagation.NewCompositeTextMapPropagator(
        		propagation.TraceContext{},
        		propagation.Baggage{},
        	))

        	return tp.Shutdown, nil
        }
        """
                .formatted(moduleName);
    }

    private String getProjectName() {
        int lastSlash = moduleName.lastIndexOf('/');
        if (lastSlash >= 0) {
            return moduleName.substring(lastSlash + 1);
        }
        return moduleName;
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("-", "_")
                .toLowerCase(Locale.ROOT);
    }
}
