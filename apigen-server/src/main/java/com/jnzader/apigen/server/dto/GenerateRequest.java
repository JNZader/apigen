package com.jnzader.apigen.server.dto;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for project generation. Contains project configuration and SQL schema. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRequest {

    /** Project configuration */
    @Valid
    @NotNull(message = "Project configuration is required")
    private ProjectConfig project;

    /**
     * Target language/framework configuration. Optional - defaults to Java/Spring Boot for backward
     * compatibility.
     */
    @Valid @Builder.Default private TargetConfig target = new TargetConfig();

    /**
     * SQL schema to generate code from. This is the CREATE TABLE statements exported from the web
     * designer. Either sql or openApiSpec must be provided.
     */
    private String sql;

    /**
     * OpenAPI specification (YAML or JSON) to generate code from. Alternative to SQL schema input.
     * Either sql or openApiSpec must be provided.
     */
    private String openApiSpec;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectConfig {

        @NotBlank(message = "Project name is required")
        private String name;

        @NotBlank(message = "Group ID is required")
        @Pattern(
                regexp = "^[a-z][a-z0-9]*+(\\.[a-z][a-z0-9]*+)*+$",
                message = "Group ID must be a valid Java package (e.g., com.example)")
        private String groupId;

        @NotBlank(message = "Artifact ID is required")
        @Pattern(
                regexp = "^[a-z][a-z0-9-]*$",
                message = "Artifact ID must be lowercase with hyphens (e.g., my-api)")
        private String artifactId;

        @Builder.Default private String javaVersion = GeneratedProjectVersions.JAVA_VERSION;

        @Builder.Default
        private String springBootVersion = GeneratedProjectVersions.SPRING_BOOT_VERSION;

        @Valid @Builder.Default private ModulesConfig modules = new ModulesConfig();

        @Valid @Builder.Default private FeaturesConfig features = new FeaturesConfig();

        @Valid @Builder.Default private DatabaseConfig database = new DatabaseConfig();

        @Valid private CorsConfig corsConfig;

        @Valid private CacheConfig cacheConfig;

        @Valid private RateLimitConfig rateLimitConfig;

        @Valid private SecurityConfigDTO securityConfig;

        /** Mail service configuration. */
        @Valid private MailConfig mailConfig;

        /** File storage configuration. */
        @Valid private StorageConfig storageConfig;

        /** Social login configuration. */
        @Valid private SocialLoginConfig socialLoginConfig;

        /** jte templates configuration. */
        @Valid private JteConfig jteConfig;

        /**
         * Optional base package override. If not provided, it will be computed from groupId and
         * artifactId. For Go projects, this should be in module format (e.g., "github.com/user").
         */
        private String basePackage;

        /**
         * Get the base package. If explicitly set, returns that value. Otherwise, computes it from
         * groupId and artifactId (Java-style: com.example.myapi).
         */
        public String getBasePackage() {
            if (basePackage != null && !basePackage.isBlank()) {
                return basePackage;
            }
            String sanitizedArtifact = artifactId.replace("-", "");
            return groupId + "." + sanitizedArtifact;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModulesConfig {
        @Builder.Default private boolean core = true;

        @Builder.Default private boolean security = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeaturesConfig {
        private Boolean hateoas;

        private Boolean swagger;

        private Boolean auditing;

        private Boolean softDelete;

        private Boolean caching;

        private Boolean docker;

        /** Enable social login generation. */
        private Boolean socialLogin;

        /** Enable password reset flow generation. */
        private Boolean passwordReset;

        /** Enable mail service generation. */
        private Boolean mailService;

        /** Enable file upload generation. */
        private Boolean fileUpload;

        /** Enable jte templates generation. */
        private Boolean jteTemplates;

        // Developer Experience (DX) Features

        /** Enable Mise task runner generation. */
        private Boolean miseTasks;

        /** Enable pre-commit hooks generation. */
        private Boolean preCommit;

        /** Enable setup scripts generation. */
        private Boolean setupScript;

        /** Enable GitHub templates generation. */
        private Boolean githubTemplates;

        /** Enable enhanced Docker Compose generation. */
        private Boolean devCompose;

        // Getters with defaults (true if null for enabled-by-default features)
        public boolean isHateoas() {
            return hateoas != null ? hateoas : true;
        }

        public boolean isSwagger() {
            return swagger != null ? swagger : true;
        }

        public boolean isAuditing() {
            return auditing != null ? auditing : true;
        }

        public boolean isSoftDelete() {
            return softDelete != null ? softDelete : true;
        }

        public boolean isCaching() {
            return caching != null ? caching : true;
        }

        public boolean isDocker() {
            return docker != null ? docker : true;
        }

        // Getters with defaults (false if null for disabled-by-default features)
        public boolean isSocialLogin() {
            return socialLogin != null ? socialLogin : false;
        }

        public boolean isPasswordReset() {
            return passwordReset != null ? passwordReset : false;
        }

        public boolean isMailService() {
            return mailService != null ? mailService : false;
        }

        public boolean isFileUpload() {
            return fileUpload != null ? fileUpload : false;
        }

        public boolean isJteTemplates() {
            return jteTemplates != null ? jteTemplates : false;
        }

        // DX Features getters with defaults (true if null)
        public boolean isMiseTasks() {
            return miseTasks != null ? miseTasks : true;
        }

        public boolean isPreCommit() {
            return preCommit != null ? preCommit : true;
        }

        public boolean isSetupScript() {
            return setupScript != null ? setupScript : true;
        }

        public boolean isGithubTemplates() {
            return githubTemplates != null ? githubTemplates : true;
        }

        public boolean isDevCompose() {
            return devCompose != null ? devCompose : true;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseConfig {
        // Database type constants
        public static final String DB_MYSQL = "mysql";
        public static final String DB_MARIADB = "mariadb";
        public static final String DB_SQLSERVER = "sqlserver";
        public static final String DB_ORACLE = "oracle";
        public static final String DB_H2 = "h2";
        public static final String DB_POSTGRESQL = "postgresql";

        /** Database type: postgresql, mysql, mariadb, h2, sqlserver, oracle */
        @Builder.Default private String type = DB_POSTGRESQL;

        @Builder.Default private String name = "appdb";

        @Builder.Default private String username = "appuser";

        /**
         * Default password placeholder for generated Docker configuration. This is a template value
         * that users are expected to override in their environment.
         */
        @Builder.Default
        @SuppressWarnings(
                "java:S2068") // This is a template default, not a hardcoded password in use
        private String password = "changeme";

        @Builder.Default private Integer port = 5432;

        // Custom getters to handle null values from JSON deserialization
        public String getType() {
            return type != null ? type : DB_POSTGRESQL;
        }

        public String getName() {
            return name != null ? name : "appdb";
        }

        public String getUsername() {
            return username != null ? username : "appuser";
        }

        public String getPassword() {
            return password != null ? password : "changeme";
        }

        public Integer getPort() {
            return port != null ? port : 5432;
        }

        /** Returns the Docker image for this database type. */
        public String getDockerImage() {
            return switch (getType().toLowerCase()) {
                case DB_MYSQL -> GeneratedProjectVersions.MYSQL_DOCKER_IMAGE;
                case DB_MARIADB -> GeneratedProjectVersions.MARIADB_DOCKER_IMAGE;
                case DB_SQLSERVER -> GeneratedProjectVersions.SQLSERVER_DOCKER_IMAGE;
                case DB_ORACLE -> GeneratedProjectVersions.ORACLE_DOCKER_IMAGE;
                case DB_H2 -> null; // H2 doesn't need a container
                default -> GeneratedProjectVersions.POSTGRES_DOCKER_IMAGE;
            };
        }

        /** Returns the JDBC driver class name. */
        public String getDriverClassName() {
            return switch (getType().toLowerCase()) {
                case DB_MYSQL -> "com.mysql.cj.jdbc.Driver";
                case DB_MARIADB -> "org.mariadb.jdbc.Driver";
                case DB_SQLSERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                case DB_ORACLE -> "oracle.jdbc.OracleDriver";
                case DB_H2 -> "org.h2.Driver";
                default -> "org.postgresql.Driver";
            };
        }

        /** Returns the default port for this database type. */
        public int getDefaultPort() {
            return switch (getType().toLowerCase()) {
                case DB_MYSQL, DB_MARIADB -> 3306;
                case DB_SQLSERVER -> 1433;
                case DB_ORACLE -> 1521;
                case DB_H2 -> 9092;
                default -> 5432;
            };
        }

        /** Returns the JDBC URL for Docker environment. */
        public String getJdbcUrl() {
            return switch (getType().toLowerCase()) {
                case DB_MYSQL ->
                        "jdbc:mysql://db:"
                                + getDefaultPort()
                                + "/"
                                + getName()
                                + "?useSSL=false&allowPublicKeyRetrieval=true";
                case DB_MARIADB -> "jdbc:mariadb://db:" + getDefaultPort() + "/" + getName();
                case DB_SQLSERVER ->
                        "jdbc:sqlserver://db:"
                                + getDefaultPort()
                                + ";databaseName="
                                + getName()
                                + ";encrypt=false";
                case DB_ORACLE -> "jdbc:oracle:thin:@db:" + getDefaultPort() + "/" + getName();
                case DB_H2 -> "jdbc:h2:mem:" + getName() + ";DB_CLOSE_DELAY=-1";
                default -> "jdbc:postgresql://db:" + getDefaultPort() + "/" + getName();
            };
        }

        /** Returns the Hibernate dialect for this database type. */
        public String getHibernateDialect() {
            return switch (getType().toLowerCase()) {
                case DB_MYSQL -> "org.hibernate.dialect.MySQLDialect";
                case DB_MARIADB -> "org.hibernate.dialect.MariaDBDialect";
                case DB_SQLSERVER -> "org.hibernate.dialect.SQLServerDialect";
                case DB_ORACLE -> "org.hibernate.dialect.OracleDialect";
                case DB_H2 -> "org.hibernate.dialect.H2Dialect";
                default -> "org.hibernate.dialect.PostgreSQLDialect";
            };
        }
    }

    /** CORS configuration. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorsConfig {
        private java.util.List<String> allowedOrigins;
        private java.util.List<String> allowedMethods;
        private java.util.List<String> allowedHeaders;
        private java.util.List<String> exposedHeaders;
        @Builder.Default private boolean allowCredentials = true;
        @Builder.Default private int maxAgeSeconds = 3600;
    }

    /** Cache configuration. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheConfig {
        @Builder.Default private String type = "local";
        private CacheEntitiesConfig entities;
        private CacheListsConfig lists;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CacheEntitiesConfig {
            @Builder.Default private int maxSize = 1000;
            @Builder.Default private int expireAfterWriteMinutes = 10;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CacheListsConfig {
            @Builder.Default private int maxSize = 100;
            @Builder.Default private int expireAfterWriteMinutes = 5;
        }
    }

    /** Rate limiting configuration. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitConfig {
        @Builder.Default private String storageMode = "IN_MEMORY";
        @Builder.Default private int requestsPerSecond = 100;
        @Builder.Default private int burstCapacity = 150;
        @Builder.Default private int authRequestsPerMinute = 10;
        @Builder.Default private int authBurstCapacity = 15;
        @Builder.Default private int blockDurationSeconds = 60;
        @Builder.Default private boolean enablePerUser = true;
        @Builder.Default private boolean enablePerEndpoint = true;
    }

    /** Security configuration DTO. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityConfigDTO {
        @Builder.Default private String mode = "jwt";
        @Builder.Default private int jwtSecretLength = 64;
        @Builder.Default private int accessTokenExpiration = 30;
        @Builder.Default private int refreshTokenExpiration = 7;
        @Builder.Default private boolean enableRefreshToken = true;
        @Builder.Default private boolean enableTokenBlacklist = true;
        @Builder.Default private int passwordMinLength = 8;
        @Builder.Default private int maxLoginAttempts = 5;
        @Builder.Default private int lockoutMinutes = 15;
    }

    /**
     * Target language/framework configuration for code generation. Specifies which generator to
     * use.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetConfig {
        /** Target programming language (e.g., "java", "kotlin", "python"). */
        @Builder.Default private String language = "java";

        /** Target framework (e.g., "spring-boot", "quarkus", "fastapi"). */
        @Builder.Default private String framework = "spring-boot";

        // Custom getters to handle null values from JSON deserialization
        public String getLanguage() {
            return language != null ? language : "java";
        }

        public String getFramework() {
            return framework != null ? framework : "spring-boot";
        }
    }

    /** Mail service configuration. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailConfig {
        /** Enable mail service generation. */
        @Builder.Default private boolean enabled = false;

        /** SMTP host. */
        @Builder.Default private String host = "smtp.example.com";

        /** SMTP port. */
        @Builder.Default private int port = 587;

        /** SMTP username. */
        private String username;

        /** Use STARTTLS. */
        @Builder.Default private boolean starttls = true;

        /** From email address. */
        @Builder.Default private String fromAddress = "noreply@example.com";

        /** From name. */
        @Builder.Default private String fromName = "Application";

        /** Generate welcome email template. */
        @Builder.Default private boolean generateWelcomeTemplate = true;

        /** Generate password reset email template. */
        @Builder.Default private boolean generatePasswordResetTemplate = true;

        /** Generate notification email template. */
        @Builder.Default private boolean generateNotificationTemplate = true;
    }

    /** File storage configuration. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageConfig {
        /** Storage type: local, s3, azure. */
        @Builder.Default private String type = "local";

        /** Local storage path (for local type). */
        @Builder.Default private String localPath = "./uploads";

        /** Maximum file size in MB. */
        @Builder.Default private int maxFileSizeMb = 10;

        /** Allowed file extensions (comma-separated). */
        @Builder.Default
        private String allowedExtensions = "jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx";

        /** S3 bucket name (for s3 type). */
        private String s3Bucket;

        /** S3 region (for s3 type). */
        @Builder.Default private String s3Region = "us-east-1";

        /** Azure container name (for azure type). */
        private String azureContainer;

        /** Azure connection string environment variable name (for azure type). */
        @Builder.Default
        private String azureConnectionStringEnv = "AZURE_STORAGE_CONNECTION_STRING";

        /** Generate file metadata entity. */
        @Builder.Default private boolean generateMetadataEntity = true;
    }

    /** Social login configuration. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialLoginConfig {
        /** Enable social login generation. */
        @Builder.Default private boolean enabled = false;

        /** Enable Google OAuth2. */
        @Builder.Default private boolean google = false;

        /** Enable GitHub OAuth2. */
        @Builder.Default private boolean github = false;

        /** Enable LinkedIn OAuth2. */
        @Builder.Default private boolean linkedin = false;

        /** Redirect URL after successful login. */
        @Builder.Default private String successRedirectUrl = "/";

        /** Redirect URL after failed login. */
        @Builder.Default private String failureRedirectUrl = "/login?error";

        /** Auto-create user on first login. */
        @Builder.Default private boolean autoCreateUser = true;

        /** Link social account to existing user by email. */
        @Builder.Default private boolean linkByEmail = true;
    }

    /** jte templates configuration. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JteConfig {
        /** Enable jte templates generation. */
        @Builder.Default private boolean enabled = false;

        /** Generate admin dashboard. */
        @Builder.Default private boolean generateAdmin = true;

        /** Generate CRUD views for entities. */
        @Builder.Default private boolean generateCrudViews = true;

        /** Include Tailwind CSS. */
        @Builder.Default private boolean includeTailwind = true;

        /** Include Alpine.js for interactivity. */
        @Builder.Default private boolean includeAlpine = true;

        /** Admin path prefix. */
        @Builder.Default private String adminPath = "/admin";
    }
}
