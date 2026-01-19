package com.jnzader.apigen.security.infrastructure.controller;

import com.jnzader.apigen.security.application.dto.AuthResponseDTO;
import com.jnzader.apigen.security.application.dto.LoginRequestDTO;
import com.jnzader.apigen.security.application.dto.RefreshTokenRequestDTO;
import com.jnzader.apigen.security.application.dto.RegisterRequestDTO;
import com.jnzader.apigen.security.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de autenticación.
 *
 * <p>Expone endpoints para login, registro, refresh y logout.
 */
@RestController
@RequestMapping("${app.api.base-path:}/v1/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticación (modo JWT)")
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
@ConditionalOnExpression("'${apigen.security.mode:jwt}'.equalsIgnoreCase('jwt')")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica un usuario y retorna tokens JWT")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Login exitoso"),
                @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
            })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Registrar usuario", description = "Crea una nueva cuenta de usuario")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Registro exitoso"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Datos inválidos o usuario ya existe")
            })
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            summary = "Refrescar token",
            description = "Obtiene nuevos tokens usando un refresh token válido")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Tokens renovados"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Refresh token inválido o expirado")
            })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @Operation(
            summary = "Cerrar sesión",
            description = "Invalida el token actual añadiéndolo a la blacklist",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Logout exitoso"),
                @ApiResponse(responseCode = "401", description = "Token inválido")
            })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            authService.logout(token);
        }

        return ResponseEntity.noContent().build();
    }
}
