package com.jnzader.apigen.core.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jnzader.apigen.core.domain.entity.audit.Revision;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("CustomRevisionListener Tests")
class CustomRevisionListenerTest {

    private CustomRevisionListener listener;
    private Revision revision;

    @BeforeEach
    void setUp() {
        listener = new CustomRevisionListener();
        revision = new Revision();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("newRevision")
    class NewRevisionTests {

        @Test
        @DisplayName("should set username from authenticated user")
        void shouldSetUsernameFromAuthenticatedUser() {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            "testuser", "password", Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            listener.newRevision(revision);

            assertThat(revision.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should set anonymous when no authentication")
        void shouldSetAnonymousWhenNoAuthentication() {
            SecurityContextHolder.clearContext();

            listener.newRevision(revision);

            assertThat(revision.getUsername()).isEqualTo("anonymous");
        }

        @Test
        @DisplayName("should set anonymous when authentication is null")
        void shouldSetAnonymousWhenAuthenticationIsNull() {
            SecurityContext context = mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(context);

            listener.newRevision(revision);

            assertThat(revision.getUsername()).isEqualTo("anonymous");
        }

        @Test
        @DisplayName("should set anonymous when not authenticated")
        void shouldSetAnonymousWhenNotAuthenticated() {
            UsernamePasswordAuthenticationToken auth =
                    mock(UsernamePasswordAuthenticationToken.class);
            when(auth.isAuthenticated()).thenReturn(false);
            SecurityContextHolder.getContext().setAuthentication(auth);

            listener.newRevision(revision);

            assertThat(revision.getUsername()).isEqualTo("anonymous");
        }

        @Test
        @DisplayName("should use principal name from authentication")
        void shouldUsePrincipalNameFromAuthentication() {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            "admin@example.com", "secret", Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            listener.newRevision(revision);

            assertThat(revision.getUsername()).isEqualTo("admin@example.com");
        }
    }
}
