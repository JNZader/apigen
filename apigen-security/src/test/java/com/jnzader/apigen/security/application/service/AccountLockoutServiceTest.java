package com.jnzader.apigen.security.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jnzader.apigen.security.domain.entity.User;
import com.jnzader.apigen.security.domain.repository.UserRepository;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.AccountLockoutProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLockoutService Unit Tests")
@Tag("security")
class AccountLockoutServiceTest {

    @Mock private UserRepository userRepository;

    private SecurityProperties securityProperties;
    private AccountLockoutProperties lockoutProperties;
    private AccountLockoutService lockoutService;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        lockoutProperties = securityProperties.getAccountLockout();
        lockoutProperties.setEnabled(true);
        lockoutProperties.setMaxFailedAttempts(5);
        lockoutProperties.setLockoutDurationMinutes(15);
        lockoutProperties.setResetAfterMinutes(30);
        lockoutProperties.setPermanentLockoutEnabled(false);

        lockoutService = new AccountLockoutService(userRepository, securityProperties);
    }

    @Nested
    @DisplayName("recordFailedAttempt()")
    class RecordFailedAttemptTests {

        @Test
        @DisplayName("should increment failed attempt count on first failure")
        void shouldIncrementFailedAttemptCountOnFirstFailure() {
            User user = createTestUser("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            lockoutService.recordFailedAttempt("testuser");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();
            assertThat(savedUser.getFailedAttemptCount()).isEqualTo(1);
            assertThat(savedUser.getLastFailedAttemptAt()).isNotNull();
            assertThat(savedUser.getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("should lock account after max failed attempts")
        void shouldLockAccountAfterMaxFailedAttempts() {
            User user = createTestUser("testuser");
            user.setFailedAttemptCount(4); // One more attempt will trigger lockout
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            lockoutService.recordFailedAttempt("testuser");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();
            assertThat(savedUser.getLockedUntil()).isNotNull();
            assertThat(savedUser.getLockedUntil()).isAfter(Instant.now());
            assertThat(savedUser.getLockoutCount()).isEqualTo(1);
            // Counter should be reset after lockout
            assertThat(savedUser.getFailedAttemptCount()).isZero();
        }

        @Test
        @DisplayName("should not record attempts when lockout is disabled")
        void shouldNotRecordAttemptsWhenDisabled() {
            lockoutProperties.setEnabled(false);

            lockoutService.recordFailedAttempt("testuser");

            verify(userRepository, never()).findByUsername(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not throw for non-existent user")
        void shouldNotThrowForNonExistentUser() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Should not throw
            lockoutService.recordFailedAttempt("nonexistent");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reset counter after inactivity period")
        void shouldResetCounterAfterInactivityPeriod() {
            User user = createTestUser("testuser");
            user.setFailedAttemptCount(3);
            // Last attempt was 35 minutes ago (beyond resetAfterMinutes=30)
            user.setLastFailedAttemptAt(Instant.now().minus(Duration.ofMinutes(35)));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            lockoutService.recordFailedAttempt("testuser");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();
            // Should be 1, not 4 (reset + 1)
            assertThat(savedUser.getFailedAttemptCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("recordSuccessfulLogin()")
    class RecordSuccessfulLoginTests {

        @Test
        @DisplayName("should reset failed attempt count on successful login")
        void shouldResetFailedAttemptCountOnSuccessfulLogin() {
            User user = createTestUser("testuser");
            user.setFailedAttemptCount(3);
            user.setLastFailedAttemptAt(Instant.now().minus(Duration.ofMinutes(5)));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            lockoutService.recordSuccessfulLogin("testuser");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();
            assertThat(savedUser.getFailedAttemptCount()).isZero();
            assertThat(savedUser.getLastFailedAttemptAt()).isNull();
            assertThat(savedUser.getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("should clear temporary lockout on successful login")
        void shouldClearTemporaryLockoutOnSuccessfulLogin() {
            User user = createTestUser("testuser");
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(10)));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            lockoutService.recordSuccessfulLogin("testuser");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();
            assertThat(savedUser.getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("should not save if no failed attempts exist")
        void shouldNotSaveIfNoFailedAttemptsExist() {
            User user = createTestUser("testuser");
            user.setFailedAttemptCount(0);
            user.setLockedUntil(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            lockoutService.recordSuccessfulLogin("testuser");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("isAccountLocked()")
    class IsAccountLockedTests {

        @Test
        @DisplayName("should return true when account is temporarily locked")
        void shouldReturnTrueWhenAccountIsTemporarilyLocked() {
            User user = createTestUser("testuser");
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(10)));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            boolean locked = lockoutService.isAccountLocked("testuser");

            assertThat(locked).isTrue();
        }

        @Test
        @DisplayName("should return true when account is permanently locked")
        void shouldReturnTrueWhenAccountIsPermanentlyLocked() {
            User user = createTestUser("testuser");
            user.setAccountNonLocked(false);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            boolean locked = lockoutService.isAccountLocked("testuser");

            assertThat(locked).isTrue();
        }

        @Test
        @DisplayName("should return false when lockout has expired")
        void shouldReturnFalseWhenLockoutHasExpired() {
            User user = createTestUser("testuser");
            user.setLockedUntil(Instant.now().minus(Duration.ofMinutes(1)));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            boolean locked = lockoutService.isAccountLocked("testuser");

            assertThat(locked).isFalse();
        }

        @Test
        @DisplayName("should return false when account is not locked")
        void shouldReturnFalseWhenAccountIsNotLocked() {
            User user = createTestUser("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            boolean locked = lockoutService.isAccountLocked("testuser");

            assertThat(locked).isFalse();
        }

        @Test
        @DisplayName("should return false for non-existent user")
        void shouldReturnFalseForNonExistentUser() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            boolean locked = lockoutService.isAccountLocked("nonexistent");

            assertThat(locked).isFalse();
        }

        @Test
        @DisplayName("should return false when lockout is disabled")
        void shouldReturnFalseWhenLockoutIsDisabled() {
            lockoutProperties.setEnabled(false);

            boolean locked = lockoutService.isAccountLocked("testuser");

            assertThat(locked).isFalse();
            verify(userRepository, never()).findByUsername(any());
        }
    }

    @Nested
    @DisplayName("unlockAccount()")
    class UnlockAccountTests {

        @Test
        @DisplayName("should unlock temporarily locked account")
        void shouldUnlockTemporarilyLockedAccount() {
            User user = createTestUser("testuser");
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(10)));
            user.setFailedAttemptCount(5);
            user.setLockoutCount(2);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            boolean unlocked = lockoutService.unlockAccount("testuser");

            assertThat(unlocked).isTrue();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();
            assertThat(savedUser.getLockedUntil()).isNull();
            assertThat(savedUser.getFailedAttemptCount()).isZero();
            assertThat(savedUser.getLockoutCount()).isZero();
            assertThat(savedUser.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("should unlock permanently locked account")
        void shouldUnlockPermanentlyLockedAccount() {
            User user = createTestUser("testuser");
            user.setAccountNonLocked(false);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            boolean unlocked = lockoutService.unlockAccount("testuser");

            assertThat(unlocked).isTrue();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();
            assertThat(savedUser.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existent user")
        void shouldReturnFalseForNonExistentUser() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            boolean unlocked = lockoutService.unlockAccount("nonexistent");

            assertThat(unlocked).isFalse();
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Permanent Lockout")
    class PermanentLockoutTests {

        @BeforeEach
        void enablePermanentLockout() {
            lockoutProperties.setPermanentLockoutEnabled(true);
            lockoutProperties.setLockoutsBeforePermanent(3);
        }

        @Test
        @DisplayName("should permanently lock account after max lockouts")
        void shouldPermanentlyLockAccountAfterMaxLockouts() {
            User user = createTestUser("testuser");
            user.setFailedAttemptCount(4);
            user.setLockoutCount(2); // One more lockout will trigger permanent lock
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            lockoutService.recordFailedAttempt("testuser");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();
            assertThat(savedUser.isAccountNonLocked()).isFalse();
            assertThat(savedUser.getLockedUntil()).isNull(); // Null indicates permanent
            assertThat(savedUser.getLockoutCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should use temporary lockout before reaching permanent threshold")
        void shouldUseTemporaryLockoutBeforePermanentThreshold() {
            User user = createTestUser("testuser");
            user.setFailedAttemptCount(4);
            user.setLockoutCount(1); // Not at permanent threshold yet
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            lockoutService.recordFailedAttempt("testuser");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();
            // accountNonLocked flag should still be true (temporary lockout uses lockedUntil)
            assertThat(savedUser.getLockedUntil()).isNotNull(); // Temporary lockout is set
            assertThat(savedUser.getLockoutCount()).isEqualTo(2);
            // Note: isAccountNonLocked() returns false because lockedUntil is set
            // The key difference is that permanent lockout sets accountNonLocked=false +
            // lockedUntil=null
        }
    }

    @Nested
    @DisplayName("getRemainingLockoutDuration()")
    class GetRemainingLockoutDurationTests {

        @Test
        @DisplayName("should return remaining duration for temporarily locked account")
        void shouldReturnRemainingDurationForTemporarilyLockedAccount() {
            User user = createTestUser("testuser");
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(10)));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            Duration remaining = lockoutService.getRemainingLockoutDuration("testuser");

            assertThat(remaining).isPositive();
            assertThat(remaining.toMinutes()).isLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("should return zero for unlocked account")
        void shouldReturnZeroForUnlockedAccount() {
            User user = createTestUser("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            Duration remaining = lockoutService.getRemainingLockoutDuration("testuser");

            assertThat(remaining).isEqualTo(Duration.ZERO);
        }

        @Test
        @DisplayName("should return very long duration for permanently locked account")
        void shouldReturnVeryLongDurationForPermanentlyLockedAccount() {
            User user = createTestUser("testuser");
            user.setAccountNonLocked(false);
            user.setLockedUntil(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            Duration remaining = lockoutService.getRemainingLockoutDuration("testuser");

            assertThat(remaining.toDays()).isGreaterThan(365);
        }
    }

    private User createTestUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setEmail(username + "@test.com");
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);
        return user;
    }
}
