package com.jnzader.apigen.security.infrastructure.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.security.infrastructure.oauth2.PKCEAuthorizationStore.AuthorizationData;
import com.jnzader.apigen.security.infrastructure.oauth2.PKCEService.CodeChallengeMethod;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PKCEAuthorizationStore}.
 *
 * <p>Tests cover the lifecycle of authorization codes in the PKCE flow.
 */
@DisplayName("PKCEAuthorizationStore")
class PKCEAuthorizationStoreTest {

    private PKCEAuthorizationStore store;

    @BeforeEach
    void setUp() {
        store = new PKCEAuthorizationStore();
    }

    @Nested
    @DisplayName("createAuthorizationCode")
    class CreateAuthorizationCode {

        @Test
        @DisplayName("should create authorization code with all data")
        void shouldCreateAuthorizationCodeWithAllData() {
            String code =
                    store.createAuthorizationCode(
                            "user123",
                            "challenge123",
                            CodeChallengeMethod.S256,
                            "client-app",
                            "https://app.com/callback",
                            "openid profile");

            assertThat(code).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("should generate unique codes for different requests")
        void shouldGenerateUniqueCodes() {
            String code1 =
                    store.createAuthorizationCode(
                            "user1",
                            "challenge1",
                            CodeChallengeMethod.S256,
                            "client1",
                            "uri1",
                            "scope1");

            String code2 =
                    store.createAuthorizationCode(
                            "user2",
                            "challenge2",
                            CodeChallengeMethod.S256,
                            "client2",
                            "uri2",
                            "scope2");

            assertThat(code1).isNotEqualTo(code2);
        }

        @Test
        @DisplayName("should increment stored code count")
        void shouldIncrementStoredCodeCount() {
            int initialCount = store.getStoredCodeCount();

            store.createAuthorizationCode(
                    "user", "challenge", CodeChallengeMethod.S256, "client", "uri", "scope");

            assertThat(store.getStoredCodeCount()).isEqualTo(initialCount + 1);
        }
    }

    @Nested
    @DisplayName("consumeAuthorizationCode")
    class ConsumeAuthorizationCode {

        @Test
        @DisplayName("should return authorization data for valid code")
        void shouldReturnAuthorizationDataForValidCode() {
            String code =
                    store.createAuthorizationCode(
                            "user123",
                            "challenge123",
                            CodeChallengeMethod.S256,
                            "client-app",
                            "https://app.com/callback",
                            "openid profile");

            Optional<AuthorizationData> result = store.consumeAuthorizationCode(code);

            assertThat(result).isPresent();
            AuthorizationData data = result.get();
            assertThat(data.userId()).isEqualTo("user123");
            assertThat(data.codeChallenge()).isEqualTo("challenge123");
            assertThat(data.challengeMethod()).isEqualTo(CodeChallengeMethod.S256);
            assertThat(data.clientId()).isEqualTo("client-app");
            assertThat(data.redirectUri()).isEqualTo("https://app.com/callback");
            assertThat(data.scopes()).isEqualTo("openid profile");
        }

        @Test
        @DisplayName("should consume code only once (single-use)")
        void shouldConsumeCodeOnlyOnce() {
            String code =
                    store.createAuthorizationCode(
                            "user",
                            "challenge",
                            CodeChallengeMethod.S256,
                            "client",
                            "uri",
                            "scope");

            // First consumption should succeed
            Optional<AuthorizationData> firstAttempt = store.consumeAuthorizationCode(code);
            assertThat(firstAttempt).isPresent();

            // Second consumption should fail
            Optional<AuthorizationData> secondAttempt = store.consumeAuthorizationCode(code);
            assertThat(secondAttempt).isEmpty();
        }

        @Test
        @DisplayName("should return empty for null code")
        void shouldReturnEmptyForNullCode() {
            Optional<AuthorizationData> result = store.consumeAuthorizationCode(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for unknown code")
        void shouldReturnEmptyForUnknownCode() {
            Optional<AuthorizationData> result = store.consumeAuthorizationCode("unknown-code");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for expired code")
        void shouldReturnEmptyForExpiredCode() {
            // Set very short expiration
            store.setCodeExpiration(Duration.ofMillis(1));

            String code =
                    store.createAuthorizationCode(
                            "user",
                            "challenge",
                            CodeChallengeMethod.S256,
                            "client",
                            "uri",
                            "scope");

            // Wait for expiration
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Optional<AuthorizationData> result = store.consumeAuthorizationCode(code);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should decrement stored code count after consumption")
        void shouldDecrementStoredCodeCountAfterConsumption() {
            String code =
                    store.createAuthorizationCode(
                            "user",
                            "challenge",
                            CodeChallengeMethod.S256,
                            "client",
                            "uri",
                            "scope");
            int countAfterCreate = store.getStoredCodeCount();

            store.consumeAuthorizationCode(code);

            assertThat(store.getStoredCodeCount()).isEqualTo(countAfterCreate - 1);
        }
    }

    @Nested
    @DisplayName("isValidCode")
    class IsValidCode {

        @Test
        @DisplayName("should return true for valid non-expired code")
        void shouldReturnTrueForValidCode() {
            String code =
                    store.createAuthorizationCode(
                            "user",
                            "challenge",
                            CodeChallengeMethod.S256,
                            "client",
                            "uri",
                            "scope");

            assertThat(store.isValidCode(code)).isTrue();
        }

        @Test
        @DisplayName("should return false for null code")
        void shouldReturnFalseForNullCode() {
            assertThat(store.isValidCode(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for unknown code")
        void shouldReturnFalseForUnknownCode() {
            assertThat(store.isValidCode("unknown")).isFalse();
        }

        @Test
        @DisplayName("should return false for expired code")
        void shouldReturnFalseForExpiredCode() {
            store.setCodeExpiration(Duration.ofMillis(1));

            String code =
                    store.createAuthorizationCode(
                            "user",
                            "challenge",
                            CodeChallengeMethod.S256,
                            "client",
                            "uri",
                            "scope");

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertThat(store.isValidCode(code)).isFalse();
        }
    }

    @Nested
    @DisplayName("revokeCodesForUser")
    class RevokeCodesForUser {

        @Test
        @DisplayName("should revoke all codes for specific user")
        void shouldRevokeAllCodesForUser() {
            String code1 =
                    store.createAuthorizationCode(
                            "user1", "c1", CodeChallengeMethod.S256, "client", "uri", "scope");
            String code2 =
                    store.createAuthorizationCode(
                            "user1", "c2", CodeChallengeMethod.S256, "client", "uri", "scope");
            String code3 =
                    store.createAuthorizationCode(
                            "user2", "c3", CodeChallengeMethod.S256, "client", "uri", "scope");

            int revokedCount = store.revokeCodesForUser("user1");

            assertThat(revokedCount).isEqualTo(2);
            assertThat(store.isValidCode(code1)).isFalse();
            assertThat(store.isValidCode(code2)).isFalse();
            assertThat(store.isValidCode(code3)).isTrue();
        }

        @Test
        @DisplayName("should return zero if no codes for user")
        void shouldReturnZeroIfNoCodesForUser() {
            int revokedCount = store.revokeCodesForUser("nonexistent-user");

            assertThat(revokedCount).isZero();
        }
    }

    @Nested
    @DisplayName("revokeCodesForClient")
    class RevokeCodesForClient {

        @Test
        @DisplayName("should revoke all codes for specific client")
        void shouldRevokeAllCodesForClient() {
            String code1 =
                    store.createAuthorizationCode(
                            "user1", "c1", CodeChallengeMethod.S256, "client-a", "uri", "scope");
            String code2 =
                    store.createAuthorizationCode(
                            "user2", "c2", CodeChallengeMethod.S256, "client-a", "uri", "scope");
            String code3 =
                    store.createAuthorizationCode(
                            "user1", "c3", CodeChallengeMethod.S256, "client-b", "uri", "scope");

            int revokedCount = store.revokeCodesForClient("client-a");

            assertThat(revokedCount).isEqualTo(2);
            assertThat(store.isValidCode(code1)).isFalse();
            assertThat(store.isValidCode(code2)).isFalse();
            assertThat(store.isValidCode(code3)).isTrue();
        }
    }

    @Nested
    @DisplayName("cleanupExpiredCodes")
    class CleanupExpiredCodes {

        @Test
        @DisplayName("should remove expired codes during cleanup")
        void shouldRemoveExpiredCodesDuringCleanup() {
            store.setCodeExpiration(Duration.ofMillis(1));

            store.createAuthorizationCode(
                    "user", "challenge", CodeChallengeMethod.S256, "client", "uri", "scope");

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int countBeforeCleanup = store.getStoredCodeCount();
            store.cleanupExpiredCodes();

            assertThat(store.getStoredCodeCount()).isLessThan(countBeforeCleanup);
        }

        @Test
        @DisplayName("should not remove valid codes during cleanup")
        void shouldNotRemoveValidCodesDuringCleanup() {
            String code =
                    store.createAuthorizationCode(
                            "user",
                            "challenge",
                            CodeChallengeMethod.S256,
                            "client",
                            "uri",
                            "scope");

            store.cleanupExpiredCodes();

            assertThat(store.isValidCode(code)).isTrue();
        }
    }

    @Nested
    @DisplayName("AuthorizationData")
    class AuthorizationDataTest {

        @Test
        @DisplayName("should correctly identify expired data")
        void shouldCorrectlyIdentifyExpiredData() {
            store.setCodeExpiration(Duration.ofMillis(1));

            String code =
                    store.createAuthorizationCode(
                            "user",
                            "challenge",
                            CodeChallengeMethod.S256,
                            "client",
                            "uri",
                            "scope");

            // Immediately after creation, it should not be expired (based on isValidCode)
            // But after waiting, it should be expired
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // The data is expired, so isValidCode should return false
            assertThat(store.isValidCode(code)).isFalse();
        }

        @Test
        @DisplayName("should store timestamps correctly")
        void shouldStoreTimestampsCorrectly() {
            String code =
                    store.createAuthorizationCode(
                            "user",
                            "challenge",
                            CodeChallengeMethod.S256,
                            "client",
                            "uri",
                            "scope");

            Optional<AuthorizationData> result = store.consumeAuthorizationCode(code);

            assertThat(result).isPresent();
            AuthorizationData data = result.get();
            assertThat(data.createdAt()).isNotNull();
            assertThat(data.expiresAt()).isNotNull();
            assertThat(data.expiresAt()).isAfter(data.createdAt());
        }
    }
}
