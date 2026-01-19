package com.jnzader.apigen.security.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jnzader.apigen.security.domain.entity.TokenBlacklist;
import com.jnzader.apigen.security.domain.entity.TokenBlacklist.BlacklistReason;
import com.jnzader.apigen.security.domain.repository.TokenBlacklistRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests para TokenBlacklistService. */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService Tests")
class TokenBlacklistServiceTest {

    @Mock private TokenBlacklistRepository repository;

    @InjectMocks private TokenBlacklistService service;

    @Captor private ArgumentCaptor<TokenBlacklist> blacklistCaptor;

    private static final String TOKEN_ID = "test-token-id-123";
    private static final String USERNAME = "testuser";
    private static final Instant EXPIRATION = Instant.now().plusSeconds(3600);

    @Nested
    @DisplayName("Blacklist Token")
    class BlacklistTokenTests {

        @Test
        @DisplayName("should add token to blacklist")
        void shouldAddTokenToBlacklist() {
            when(repository.existsByTokenId(TOKEN_ID)).thenReturn(false);
            when(repository.save(any(TokenBlacklist.class))).thenAnswer(i -> i.getArgument(0));

            service.blacklistToken(TOKEN_ID, USERNAME, EXPIRATION, BlacklistReason.LOGOUT);

            verify(repository).save(blacklistCaptor.capture());
            TokenBlacklist saved = blacklistCaptor.getValue();

            assertThat(saved.getTokenId()).isEqualTo(TOKEN_ID);
            assertThat(saved.getUsername()).isEqualTo(USERNAME);
            assertThat(saved.getExpiration()).isEqualTo(EXPIRATION);
            assertThat(saved.getReason()).isEqualTo(BlacklistReason.LOGOUT);
            assertThat(saved.getBlacklistedAt()).isNotNull();
        }

        @Test
        @DisplayName("should not duplicate already blacklisted token")
        void shouldNotDuplicateAlreadyBlacklistedToken() {
            when(repository.existsByTokenId(TOKEN_ID)).thenReturn(true);

            service.blacklistToken(TOKEN_ID, USERNAME, EXPIRATION, BlacklistReason.LOGOUT);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should blacklist with different reasons")
        void shouldBlacklistWithDifferentReasons() {
            when(repository.existsByTokenId(anyString())).thenReturn(false);
            when(repository.save(any(TokenBlacklist.class))).thenAnswer(i -> i.getArgument(0));

            service.blacklistToken(
                    "token-1", USERNAME, EXPIRATION, BlacklistReason.PASSWORD_CHANGE);
            service.blacklistToken("token-2", USERNAME, EXPIRATION, BlacklistReason.ADMIN_REVOKE);
            service.blacklistToken(
                    "token-3", USERNAME, EXPIRATION, BlacklistReason.SECURITY_BREACH);

            verify(repository, times(3)).save(blacklistCaptor.capture());

            assertThat(blacklistCaptor.getAllValues())
                    .extracting(TokenBlacklist::getReason)
                    .containsExactly(
                            BlacklistReason.PASSWORD_CHANGE,
                            BlacklistReason.ADMIN_REVOKE,
                            BlacklistReason.SECURITY_BREACH);
        }
    }

    @Nested
    @DisplayName("Check Blacklist")
    class CheckBlacklistTests {

        @Test
        @DisplayName("should return true for blacklisted token")
        void shouldReturnTrueForBlacklistedToken() {
            when(repository.existsByTokenId(TOKEN_ID)).thenReturn(true);

            assertThat(service.isBlacklisted(TOKEN_ID)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-blacklisted token")
        void shouldReturnFalseForNonBlacklistedToken() {
            when(repository.existsByTokenId(TOKEN_ID)).thenReturn(false);

            assertThat(service.isBlacklisted(TOKEN_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("Revoke User Tokens")
    class RevokeUserTokensTests {

        @Test
        @DisplayName("should revoke all user tokens")
        void shouldRevokeAllUserTokens() {
            when(repository.deleteByUsername(USERNAME)).thenReturn(5);

            service.revokeAllUserTokens(USERNAME);

            verify(repository).deleteByUsername(USERNAME);
        }
    }

    @Nested
    @DisplayName("Cleanup Expired Tokens")
    class CleanupExpiredTokensTests {

        @Test
        @DisplayName("should cleanup expired tokens")
        void shouldCleanupExpiredTokens() {
            when(repository.deleteExpiredTokens(any(Instant.class))).thenReturn(10);

            service.cleanupExpiredTokens();

            verify(repository).deleteExpiredTokens(any(Instant.class));
        }

        @Test
        @DisplayName("should handle no expired tokens")
        void shouldHandleNoExpiredTokens() {
            when(repository.deleteExpiredTokens(any(Instant.class))).thenReturn(0);

            service.cleanupExpiredTokens();

            verify(repository).deleteExpiredTokens(any(Instant.class));
        }
    }
}
