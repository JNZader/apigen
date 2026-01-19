package com.jnzader.apigen.core.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ConfigurationValidator Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfigurationValidatorTest {

    @Mock private Environment environment;

    @Mock private ApplicationArguments args;

    private ConfigurationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConfigurationValidator(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[] {});
    }

    private void setValidatorFields(
            String apiVersion,
            String apiBasePath,
            String datasourceUrl,
            int cacheMaxSize,
            String corsOrigins,
            int rateLimit,
            String profile) {
        ReflectionTestUtils.setField(validator, "apiVersion", apiVersion);
        ReflectionTestUtils.setField(validator, "apiBasePath", apiBasePath);
        ReflectionTestUtils.setField(validator, "datasourceUrl", datasourceUrl);
        ReflectionTestUtils.setField(validator, "cacheEntitiesMaxSize", cacheMaxSize);
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", corsOrigins);
        ReflectionTestUtils.setField(validator, "rateLimitMaxRequests", rateLimit);
        ReflectionTestUtils.setField(validator, "activeProfile", profile);
    }

    @Nested
    @DisplayName("Test profile")
    class TestProfileTests {

        @Test
        @DisplayName("should skip validation for test profile")
        void shouldSkipValidationForTestProfile() {
            setValidatorFields("", "", "", 0, "", 0, "test");

            assertThatCode(() -> validator.run(args)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should skip validation when test is in active profiles")
        void shouldSkipValidationWhenTestInActiveProfiles() {
            when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});
            setValidatorFields("", "", "", 0, "", 0, "dev");

            assertThatCode(() -> validator.run(args)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Critical validations")
    class CriticalValidationsTests {

        @Test
        @DisplayName("should fail when apiVersion is blank")
        void shouldFailWhenApiVersionIsBlank() {
            setValidatorFields("", "/api", "jdbc:postgresql://localhost/db", 100, "*", 100, "dev");

            assertThatThrownBy(() -> validator.run(args))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("app.api.version");
        }

        @Test
        @DisplayName("should fail when apiBasePath is blank")
        void shouldFailWhenApiBasePathIsBlank() {
            setValidatorFields("v1", "", "jdbc:postgresql://localhost/db", 100, "*", 100, "dev");

            assertThatThrownBy(() -> validator.run(args))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("app.api.base-path");
        }

        @Test
        @DisplayName("should fail when apiBasePath does not start with slash")
        void shouldFailWhenApiBasePathDoesNotStartWithSlash() {
            setValidatorFields("v1", "api", "jdbc:postgresql://localhost/db", 100, "*", 100, "dev");

            assertThatThrownBy(() -> validator.run(args))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must start with '/'");
        }

        @Test
        @DisplayName("should fail when datasourceUrl is blank")
        void shouldFailWhenDatasourceUrlIsBlank() {
            setValidatorFields("v1", "/api", "", 100, "*", 100, "dev");

            assertThatThrownBy(() -> validator.run(args))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("spring.datasource.url");
        }

        @Test
        @DisplayName("should fail when datasourceUrl contains localhost in production")
        void shouldFailWhenDatasourceUrlContainsLocalhostInProduction() {
            setValidatorFields(
                    "v1", "/api", "jdbc:postgresql://localhost/db", 100, "*", 100, "prod");
            when(environment.getProperty("spring.jpa.hibernate.ddl-auto", ""))
                    .thenReturn("validate");
            when(environment.getProperty("spring.jpa.show-sql", Boolean.class, false))
                    .thenReturn(false);
            when(environment.getProperty("logging.level.root", "INFO")).thenReturn("INFO");

            assertThatThrownBy(() -> validator.run(args))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("localhost");
        }
    }

    @Nested
    @DisplayName("Production validations")
    class ProductionValidationsTests {

        @Test
        @DisplayName("should fail when ddl-auto is update in production")
        void shouldFailWhenDdlAutoIsUpdateInProduction() {
            setValidatorFields(
                    "v1", "/api", "jdbc:postgresql://prod-host/db", 100, "*", 100, "prod");
            when(environment.getProperty("spring.jpa.hibernate.ddl-auto", "")).thenReturn("update");
            when(environment.getProperty("spring.jpa.show-sql", Boolean.class, false))
                    .thenReturn(false);
            when(environment.getProperty("logging.level.root", "INFO")).thenReturn("INFO");

            assertThatThrownBy(() -> validator.run(args))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ddl-auto");
        }

        @Test
        @DisplayName("should fail when ddl-auto is create in production")
        void shouldFailWhenDdlAutoIsCreateInProduction() {
            setValidatorFields(
                    "v1", "/api", "jdbc:postgresql://prod-host/db", 100, "*", 100, "prod");
            when(environment.getProperty("spring.jpa.hibernate.ddl-auto", "")).thenReturn("create");
            when(environment.getProperty("spring.jpa.show-sql", Boolean.class, false))
                    .thenReturn(false);
            when(environment.getProperty("logging.level.root", "INFO")).thenReturn("INFO");

            assertThatThrownBy(() -> validator.run(args))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ddl-auto");
        }

        @Test
        @DisplayName("should pass when ddl-auto is validate in production")
        void shouldPassWhenDdlAutoIsValidateInProduction() {
            setValidatorFields(
                    "v1",
                    "/api",
                    "jdbc:postgresql://prod-host/db",
                    100,
                    "https://example.com",
                    100,
                    "production");
            when(environment.getProperty("spring.jpa.hibernate.ddl-auto", ""))
                    .thenReturn("validate");
            when(environment.getProperty("spring.jpa.show-sql", Boolean.class, false))
                    .thenReturn(false);
            when(environment.getProperty("logging.level.root", "INFO")).thenReturn("INFO");

            assertThatCode(() -> validator.run(args)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Successful validation")
    class SuccessfulValidationTests {

        @Test
        @DisplayName("should pass with valid configuration")
        void shouldPassWithValidConfiguration() {
            setValidatorFields(
                    "v1",
                    "/api",
                    "jdbc:postgresql://localhost/db",
                    100,
                    "http://localhost:3000",
                    100,
                    "dev");
            when(environment.getProperty("spring.datasource.username", ""))
                    .thenReturn("custom_user");

            assertThatCode(() -> validator.run(args)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass with minimal configuration in development")
        void shouldPassWithMinimalConfigurationInDevelopment() {
            setValidatorFields("v1", "/api", "jdbc:postgresql://localhost/db", 0, "", 0, "local");
            when(environment.getProperty("spring.datasource.username", ""))
                    .thenReturn("apigen_user");

            assertThatCode(() -> validator.run(args)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Profile detection")
    class ProfileDetectionTests {

        @Test
        @DisplayName("should treat 'prod' as production")
        void shouldTreatProdAsProduction() {
            setValidatorFields(
                    "v1", "/api", "jdbc:postgresql://localhost/db", 100, "*", 100, "prod");
            when(environment.getProperty("spring.jpa.hibernate.ddl-auto", ""))
                    .thenReturn("validate");
            when(environment.getProperty("spring.jpa.show-sql", Boolean.class, false))
                    .thenReturn(false);
            when(environment.getProperty("logging.level.root", "INFO")).thenReturn("INFO");

            assertThatThrownBy(() -> validator.run(args)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should treat 'production' as production")
        void shouldTreatProductionAsProduction() {
            setValidatorFields(
                    "v1", "/api", "jdbc:postgresql://localhost/db", 100, "*", 100, "production");
            when(environment.getProperty("spring.jpa.hibernate.ddl-auto", ""))
                    .thenReturn("validate");
            when(environment.getProperty("spring.jpa.show-sql", Boolean.class, false))
                    .thenReturn(false);
            when(environment.getProperty("logging.level.root", "INFO")).thenReturn("INFO");

            assertThatThrownBy(() -> validator.run(args)).isInstanceOf(IllegalStateException.class);
        }
    }
}
