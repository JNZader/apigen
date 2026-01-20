package com.jnzader.apigen.security.application.service;

import com.jnzader.apigen.core.domain.exception.DuplicateResourceException;
import com.jnzader.apigen.core.domain.exception.ResourceNotFoundException;
import com.jnzader.apigen.core.domain.exception.ValidationException;
import com.jnzader.apigen.security.application.dto.AuthResponseDTO;
import com.jnzader.apigen.security.application.dto.LoginRequestDTO;
import com.jnzader.apigen.security.application.dto.RefreshTokenRequestDTO;
import com.jnzader.apigen.security.application.dto.RegisterRequestDTO;
import com.jnzader.apigen.security.domain.entity.Permission;
import com.jnzader.apigen.security.domain.entity.Role;
import com.jnzader.apigen.security.domain.entity.TokenBlacklist.BlacklistReason;
import com.jnzader.apigen.security.domain.entity.User;
import com.jnzader.apigen.security.domain.repository.RoleRepository;
import com.jnzader.apigen.security.domain.repository.UserRepository;
import com.jnzader.apigen.security.infrastructure.jwt.JwtService;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación.
 *
 * <p>Maneja login, registro y refresh de tokens.
 */
@Service
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
@ConditionalOnExpression("'${apigen.security.mode:jwt}'.equalsIgnoreCase('jwt')")
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /** Autentica un usuario y retorna tokens JWT. */
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user =
                userRepository
                        .findActiveByUsername(request.username())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Usuario no encontrado: " + request.username()));

        return generateAuthResponse(user);
    }

    /** Registra un nuevo usuario. */
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        // Validar que no exista el usuario
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new DuplicateResourceException(
                    "El nombre de usuario ya existe: " + request.username());
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateResourceException("El email ya está registrado: " + request.email());
        }

        // Obtener rol por defecto
        Role defaultRole =
                roleRepository
                        .findByName(DEFAULT_ROLE)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Rol por defecto no encontrado: " + DEFAULT_ROLE));

        // Crear usuario
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(defaultRole);
        user.setEnabled(true);

        user = userRepository.save(user);

        return generateAuthResponse(user);
    }

    /**
     * Refresca el token de acceso usando un refresh token válido.
     *
     * <p>Implementa rotación de refresh tokens: el token usado se invalida y se emite uno nuevo
     * junto con el access token.
     */
    @Transactional
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String refreshToken = request.refreshToken();

        // Validar que sea un refresh token
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new ValidationException("Token inválido: no es un refresh token");
        }

        // Validar estructura del token
        if (!jwtService.isTokenStructureValid(refreshToken)) {
            throw new ValidationException("Refresh token inválido o expirado");
        }

        // Verificar que el token no esté en blacklist
        String tokenId = jwtService.extractTokenId(refreshToken);
        if (tokenBlacklistService.isBlacklisted(tokenId)) {
            throw new ValidationException("Refresh token ya fue utilizado o revocado");
        }

        // Extraer username del token
        String username = jwtService.extractUsername(refreshToken);
        User user =
                userRepository
                        .findActiveByUsername(username)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Usuario no encontrado: " + username));

        // ROTACIÓN: Invalidar el refresh token usado (single-use)
        tokenBlacklistService.blacklistToken(
                tokenId,
                username,
                jwtService.extractExpiration(refreshToken),
                BlacklistReason.TOKEN_ROTATED);

        // Generar nuevos tokens (access + refresh)
        return generateAuthResponse(user);
    }

    /** Genera la respuesta de autenticación con tokens. */
    private AuthResponseDTO generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthResponseDTO.UserInfoDTO userInfo =
                new AuthResponseDTO.UserInfoDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole().getName(),
                        user.getRole().getPermissions().stream()
                                .map(Permission::getName)
                                .collect(Collectors.toUnmodifiableSet()));

        return new AuthResponseDTO(
                accessToken, refreshToken, jwtService.extractExpiration(accessToken), userInfo);
    }

    /**
     * Invalida un token añadiéndolo a la blacklist.
     *
     * @param token Token JWT a invalidar
     */
    @Transactional
    public void logout(String token) {
        String tokenId = jwtService.extractTokenId(token);
        String username = jwtService.extractUsername(token);
        Instant expiration = jwtService.extractExpiration(token);

        tokenBlacklistService.blacklistToken(tokenId, username, expiration, BlacklistReason.LOGOUT);
    }

    /**
     * Invalida todos los tokens de un usuario. Útil cuando el usuario cambia su contraseña.
     *
     * @param username Nombre de usuario
     */
    @Transactional
    public void revokeAllUserTokens(String username) {
        tokenBlacklistService.revokeAllUserTokens(username);
    }
}
