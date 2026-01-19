package com.jnzader.apigen.security.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.jnzader.apigen.security.domain.entity.Permission;
import com.jnzader.apigen.security.domain.entity.Role;
import com.jnzader.apigen.security.domain.entity.User;
import com.jnzader.apigen.security.domain.repository.UserRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Permission permission = new Permission();
        permission.setId(1L);
        permission.setName("READ");

        Set<Permission> permissions = new HashSet<>();
        permissions.add(permission);

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");
        role.setPermissions(permissions);

        testUser = new User("testuser", "password", "test@example.com", role);
        testUser.setId(1L);
        testUser.setEnabled(true);
        testUser.setEstado(true);
    }

    @Test
    @DisplayName("should load user by username when user exists and is active")
    void shouldLoadUserByUsernameWhenUserExistsAndActive() {
        when(userRepository.findActiveByUsername("testuser")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        verify(userRepository).findActiveByUsername("testuser");
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when user not found")
    void shouldThrowUsernameNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findActiveByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");

        verify(userRepository).findActiveByUsername("unknown");
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when user is inactive")
    void shouldThrowUsernameNotFoundExceptionWhenUserInactive() {
        when(userRepository.findActiveByUsername("inactive")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("inactive"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("inactivo");

        verify(userRepository).findActiveByUsername("inactive");
    }
}
