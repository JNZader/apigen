package com.jnzader.apigen.security.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SecurityProperties Tests")
class SecurityPropertiesTest {

    private SecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have security disabled by default")
        void shouldHaveSecurityDisabledByDefault() {
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should have JWT mode by default")
        void shouldHaveJwtModeByDefault() {
            assertThat(properties.getMode()).isEqualTo(SecurityProperties.AuthMode.JWT);
        }

        @Test
        @DisplayName("Should have default JWT properties")
        void shouldHaveDefaultJwtProperties() {
            SecurityProperties.JwtProperties jwt = properties.getJwt();

            assertThat(jwt)
                    .isNotNull()
                    .extracting(
                            SecurityProperties.JwtProperties::getExpirationMinutes,
                            SecurityProperties.JwtProperties::getRefreshExpirationMinutes,
                            SecurityProperties.JwtProperties::getIssuer)
                    .containsExactly(15, 10080, "apigen");
        }

        @Test
        @DisplayName("Should have default OAuth2 properties")
        void shouldHaveDefaultOAuth2Properties() {
            SecurityProperties.OAuth2Properties oauth2 = properties.getOauth2();

            assertThat(oauth2)
                    .isNotNull()
                    .extracting(
                            SecurityProperties.OAuth2Properties::getRolesClaim,
                            SecurityProperties.OAuth2Properties::getRolePrefix,
                            SecurityProperties.OAuth2Properties::getUsernameClaim)
                    .containsExactly("permissions", "ROLE_", "sub");
        }
    }

    @Nested
    @DisplayName("isJwtMode()")
    class IsJwtModeTests {

        @Test
        @DisplayName("Should return false when security is disabled")
        void shouldReturnFalseWhenSecurityIsDisabled() {
            properties.setEnabled(false);
            properties.setMode(SecurityProperties.AuthMode.JWT);

            assertThat(properties.isJwtMode()).isFalse();
        }

        @Test
        @DisplayName("Should return true when security is enabled and mode is JWT")
        void shouldReturnTrueWhenSecurityIsEnabledAndModeIsJwt() {
            properties.setEnabled(true);
            properties.setMode(SecurityProperties.AuthMode.JWT);

            assertThat(properties.isJwtMode()).isTrue();
        }

        @Test
        @DisplayName("Should return false when mode is OAuth2")
        void shouldReturnFalseWhenModeIsOAuth2() {
            properties.setEnabled(true);
            properties.setMode(SecurityProperties.AuthMode.OAUTH2);

            assertThat(properties.isJwtMode()).isFalse();
        }
    }

    @Nested
    @DisplayName("isOAuth2Mode()")
    class IsOAuth2ModeTests {

        @Test
        @DisplayName("Should return false when security is disabled")
        void shouldReturnFalseWhenSecurityIsDisabled() {
            properties.setEnabled(false);
            properties.setMode(SecurityProperties.AuthMode.OAUTH2);

            assertThat(properties.isOAuth2Mode()).isFalse();
        }

        @Test
        @DisplayName("Should return true when security is enabled and mode is OAuth2")
        void shouldReturnTrueWhenSecurityIsEnabledAndModeIsOAuth2() {
            properties.setEnabled(true);
            properties.setMode(SecurityProperties.AuthMode.OAUTH2);

            assertThat(properties.isOAuth2Mode()).isTrue();
        }

        @Test
        @DisplayName("Should return false when mode is JWT")
        void shouldReturnFalseWhenModeIsJwt() {
            properties.setEnabled(true);
            properties.setMode(SecurityProperties.AuthMode.JWT);

            assertThat(properties.isOAuth2Mode()).isFalse();
        }
    }

    @Nested
    @DisplayName("validate() - Security Disabled")
    class ValidateSecurityDisabledTests {

        @Test
        @DisplayName("Should not throw when security is disabled")
        void shouldNotThrowWhenSecurityIsDisabled() {
            properties.setEnabled(false);

            // Should not throw
            properties.validate();
        }
    }

    @Nested
    @DisplayName("validate() - JWT Mode")
    class ValidateJwtModeTests {

        @ParameterizedTest(name = "Should throw when JWT secret is: {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "short-secret"})
        @DisplayName("Should throw when JWT secret is invalid")
        void shouldThrowWhenJwtSecretIsInvalid(String secret) {
            properties.setEnabled(true);
            properties.setMode(SecurityProperties.AuthMode.JWT);
            properties.getJwt().setSecret(secret);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should not throw when JWT secret is valid")
        void shouldNotThrowWhenJwtSecretIsValid() {
            properties.setEnabled(true);
            properties.setMode(SecurityProperties.AuthMode.JWT);
            // 32+ bytes secret
            properties
                    .getJwt()
                    .setSecret("this-is-a-very-long-secure-secret-key-that-is-at-least-32-bytes");

            // Should not throw
            properties.validate();
        }

        @Test
        @DisplayName("Should warn for default-looking secrets")
        void shouldWarnForDefaultLookingSecrets() {
            properties.setEnabled(true);
            properties.setMode(SecurityProperties.AuthMode.JWT);
            // Contains "default" but is long enough
            properties
                    .getJwt()
                    .setSecret("this-is-a-default-secret-key-that-is-long-enough-for-testing");

            // Should not throw, just warn
            properties.validate();
        }
    }

    @Nested
    @DisplayName("validate() - OAuth2 Mode")
    class ValidateOAuth2ModeTests {

        @Test
        @DisplayName("Should throw when issuer URI is null")
        void shouldThrowWhenIssuerUriIsNull() {
            properties.setEnabled(true);
            properties.setMode(SecurityProperties.AuthMode.OAUTH2);
            properties.getOauth2().setIssuerUri(null);

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("issuer-uri");
        }

        @Test
        @DisplayName("Should throw when issuer URI is blank")
        void shouldThrowWhenIssuerUriIsBlank() {
            properties.setEnabled(true);
            properties.setMode(SecurityProperties.AuthMode.OAUTH2);
            properties.getOauth2().setIssuerUri("   ");

            assertThatThrownBy(() -> properties.validate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("issuer-uri");
        }

        @Test
        @DisplayName("Should not throw when issuer URI is valid")
        void shouldNotThrowWhenIssuerUriIsValid() {
            properties.setEnabled(true);
            properties.setMode(SecurityProperties.AuthMode.OAUTH2);
            properties.getOauth2().setIssuerUri("https://auth.example.com/");

            // Should not throw
            properties.validate();
        }
    }

    @Nested
    @DisplayName("JwtProperties")
    class JwtPropertiesTests {

        @Test
        @DisplayName("Should set and get secret")
        void shouldSetAndGetSecret() {
            SecurityProperties.JwtProperties jwt = new SecurityProperties.JwtProperties();
            jwt.setSecret("my-secret");

            assertThat(jwt.getSecret()).isEqualTo("my-secret");
        }

        @Test
        @DisplayName("Should set and get expiration minutes")
        void shouldSetAndGetExpirationMinutes() {
            SecurityProperties.JwtProperties jwt = new SecurityProperties.JwtProperties();
            jwt.setExpirationMinutes(30);

            assertThat(jwt.getExpirationMinutes()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should set and get refresh expiration minutes")
        void shouldSetAndGetRefreshExpirationMinutes() {
            SecurityProperties.JwtProperties jwt = new SecurityProperties.JwtProperties();
            jwt.setRefreshExpirationMinutes(1440);

            assertThat(jwt.getRefreshExpirationMinutes()).isEqualTo(1440);
        }

        @Test
        @DisplayName("Should set and get issuer")
        void shouldSetAndGetIssuer() {
            SecurityProperties.JwtProperties jwt = new SecurityProperties.JwtProperties();
            jwt.setIssuer("my-app");

            assertThat(jwt.getIssuer()).isEqualTo("my-app");
        }
    }

    @Nested
    @DisplayName("OAuth2Properties")
    class OAuth2PropertiesTests {

        @Test
        @DisplayName("Should set and get issuer URI")
        void shouldSetAndGetIssuerUri() {
            SecurityProperties.OAuth2Properties oauth2 = new SecurityProperties.OAuth2Properties();
            oauth2.setIssuerUri("https://auth.example.com/");

            assertThat(oauth2.getIssuerUri()).isEqualTo("https://auth.example.com/");
        }

        @Test
        @DisplayName("Should set and get audience")
        void shouldSetAndGetAudience() {
            SecurityProperties.OAuth2Properties oauth2 = new SecurityProperties.OAuth2Properties();
            oauth2.setAudience("my-api");

            assertThat(oauth2.getAudience()).isEqualTo("my-api");
        }

        @Test
        @DisplayName("Should set and get roles claim")
        void shouldSetAndGetRolesClaim() {
            SecurityProperties.OAuth2Properties oauth2 = new SecurityProperties.OAuth2Properties();
            oauth2.setRolesClaim("realm_access.roles");

            assertThat(oauth2.getRolesClaim()).isEqualTo("realm_access.roles");
        }

        @Test
        @DisplayName("Should set and get role prefix")
        void shouldSetAndGetRolePrefix() {
            SecurityProperties.OAuth2Properties oauth2 = new SecurityProperties.OAuth2Properties();
            oauth2.setRolePrefix("SCOPE_");

            assertThat(oauth2.getRolePrefix()).isEqualTo("SCOPE_");
        }

        @Test
        @DisplayName("Should set and get username claim")
        void shouldSetAndGetUsernameClaim() {
            SecurityProperties.OAuth2Properties oauth2 = new SecurityProperties.OAuth2Properties();
            oauth2.setUsernameClaim("email");

            assertThat(oauth2.getUsernameClaim()).isEqualTo("email");
        }
    }

    @Nested
    @DisplayName("Setters")
    class SettersTests {

        @Test
        @DisplayName("Should set enabled")
        void shouldSetEnabled() {
            properties.setEnabled(true);

            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should set mode")
        void shouldSetMode() {
            properties.setMode(SecurityProperties.AuthMode.OAUTH2);

            assertThat(properties.getMode()).isEqualTo(SecurityProperties.AuthMode.OAUTH2);
        }

        @Test
        @DisplayName("Should set JWT properties")
        void shouldSetJwtProperties() {
            SecurityProperties.JwtProperties jwt = new SecurityProperties.JwtProperties();
            jwt.setSecret("new-secret");
            properties.setJwt(jwt);

            assertThat(properties.getJwt().getSecret()).isEqualTo("new-secret");
        }

        @Test
        @DisplayName("Should set OAuth2 properties")
        void shouldSetOAuth2Properties() {
            SecurityProperties.OAuth2Properties oauth2 = new SecurityProperties.OAuth2Properties();
            oauth2.setIssuerUri("https://new-issuer.com/");
            properties.setOauth2(oauth2);

            assertThat(properties.getOauth2().getIssuerUri()).isEqualTo("https://new-issuer.com/");
        }
    }

    @Nested
    @DisplayName("AuthMode Enum")
    class AuthModeEnumTests {

        @Test
        @DisplayName("Should have JWT value")
        void shouldHaveJwtValue() {
            assertThat(SecurityProperties.AuthMode.JWT)
                    .isNotNull()
                    .extracting(SecurityProperties.AuthMode::name)
                    .isEqualTo("JWT");
        }

        @Test
        @DisplayName("Should have OAUTH2 value")
        void shouldHaveOAuth2Value() {
            assertThat(SecurityProperties.AuthMode.OAUTH2)
                    .isNotNull()
                    .extracting(SecurityProperties.AuthMode::name)
                    .isEqualTo("OAUTH2");
        }

        @Test
        @DisplayName("Should have exactly 2 values")
        void shouldHaveExactlyTwoValues() {
            assertThat(SecurityProperties.AuthMode.values()).hasSize(2);
        }
    }
}
