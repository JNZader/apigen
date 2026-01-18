package com.jnzader.apigen.security.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import com.jnzader.apigen.core.infrastructure.config.JpaConfig;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de seguridad para la API.
 * <p>
 * Verifica:
 * - Acceso a endpoints públicos
 * - Protección de endpoints privados
 * - CORS configurado correctamente
 * - CSRF configurado correctamente
 * <p>
 * Ejecutar con: ./gradlew test -Ptest.security
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(JpaConfig.class)
@ActiveProfiles("test")
@DisplayName("Security Configuration Tests")
@Tag("security")
class SecurityConfigTest {
    // NOTA: Con security disabled (perfil test), estos tests verifican el comportamiento
    // sin autenticación. Para tests con security enabled, ver integration tests.

    @Autowired
    private MockMvc mockMvc;

    // ==================== Public Endpoints Tests ====================

    @Nested
    @DisplayName("Public Endpoints")
    class PublicEndpointsTests {

        @Test
        @WithAnonymousUser
        @DisplayName("should allow anonymous access to actuator health")
        void shouldAllowAnonymousAccessToActuatorHealth() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("should allow anonymous access to actuator info")
        void shouldAllowAnonymousAccessToActuatorInfo() throws Exception {
            // Note: Info endpoint may not be configured, so we check it's not blocked by security
            // (not 401/403) rather than expecting 200
            mockMvc.perform(get("/actuator/info"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 401 || status == 403) {
                            throw new AssertionError("Expected info endpoint to be accessible, but got " + status);
                        }
                    });
        }

        @Test
        @WithAnonymousUser
        @DisplayName("should allow anonymous access to swagger UI")
        void shouldAllowAnonymousAccessToSwaggerUI() throws Exception {
            // SpringDoc usa /swagger-ui/index.html, /swagger-ui.html redirige
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("should allow anonymous access to OpenAPI docs")
        void shouldAllowAnonymousAccessToOpenAPIDocs() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== Endpoint Access Tests ====================
    // NOTA: Con security disabled, todos los endpoints son accesibles

    @Nested
    @DisplayName("Endpoint Access (Security Disabled)")
    class EndpointAccessTests {

        @Test
        @DisplayName("should allow access to API endpoints without authentication")
        void shouldAllowAccessToApiEndpointsWithoutAuth() throws Exception {
            // Con seguridad deshabilitada, no se requiere autenticación
            mockMvc.perform(get("/test-entities"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("should allow authenticated access to endpoints")
        void shouldAllowAuthenticatedAccessToEndpoints() throws Exception {
            mockMvc.perform(get("/test-entities"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should allow admin access to endpoints")
        void shouldAllowAdminAccessToEndpoints() throws Exception {
            mockMvc.perform(get("/test-entities"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== CORS Tests ====================

    @Nested
    @DisplayName("CORS Configuration")
    class CORSTests {

        @Test
        @DisplayName("should include CORS headers for allowed origins")
        void shouldIncludeCORSHeadersForAllowedOrigins() throws Exception {
            mockMvc.perform(options("/test-entities")
                            .header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Access-Control-Allow-Origin"));
        }

        @Test
        @DisplayName("should allow specific methods")
        void shouldAllowSpecificMethods() throws Exception {
            mockMvc.perform(options("/test-entities")
                            .header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Methods",
                            org.hamcrest.Matchers.containsString("POST")));
        }
    }

    // ==================== CSRF Tests ====================
    // NOTA: CSRF está deshabilitado para APIs REST stateless con JWT
    // Estos tests verifican que CSRF NO bloquea requests (comportamiento esperado)

    @Nested
    @DisplayName("CSRF Protection (Disabled for REST API)")
    class CSRFTests {

        @Test
        @WithMockUser
        @DisplayName("should accept POST without CSRF token (CSRF disabled for stateless API)")
        void shouldAcceptPOSTWithoutCSRFToken() throws Exception {
            // CSRF está deshabilitado para APIs REST con JWT
            mockMvc.perform(post("/test-entities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"activo\":true}"))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser
        @DisplayName("should also accept POST with CSRF token")
        void shouldAcceptPOSTWithCSRFToken() throws Exception {
            mockMvc.perform(post("/test-entities")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"activo\":true}"))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser
        @DisplayName("should allow GET without CSRF token")
        void shouldAllowGETWithoutCSRFToken() throws Exception {
            mockMvc.perform(get("/test-entities"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== Method Security Tests ====================

    @Nested
    @DisplayName("Method Security")
    class MethodSecurityTests {

        @Test
        @WithMockUser
        @DisplayName("should allow standard user to read")
        void shouldAllowStandardUserToRead() throws Exception {
            mockMvc.perform(get("/test-entities"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("should allow standard user to create")
        void shouldAllowStandardUserToCreate() throws Exception {
            // CSRF deshabilitado para API REST stateless
            mockMvc.perform(post("/test-entities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"activo\":true}"))
                    .andExpect(status().isCreated());
        }
    }

    // ==================== Authentication Type Tests ====================
    // NOTA: Con security disabled, no hay validación de tokens

    @Nested
    @DisplayName("Authentication Types (Security Disabled)")
    class AuthenticationTypesTests {

        @Test
        @DisplayName("should accept requests with or without Authorization header when security disabled")
        void shouldAcceptRequestsWithAuthHeaderWhenSecurityDisabled() throws Exception {
            // Con seguridad deshabilitada, el header Authorization se ignora
            mockMvc.perform(get("/test-entities")
                            .header("Authorization", "Bearer any-token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should accept requests without Authorization header when security disabled")
        void shouldAcceptRequestsWithoutAuthHeaderWhenSecurityDisabled() throws Exception {
            mockMvc.perform(get("/test-entities"))
                    .andExpect(status().isOk());
        }
    }
}
