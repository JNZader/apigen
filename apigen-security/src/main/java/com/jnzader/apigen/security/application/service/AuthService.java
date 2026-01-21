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
 * Authentication service.
 *
 * <p>Handles login, registration and token refresh.
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

    /** Authenticates a user and returns JWT tokens. */
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
                                                "User not found: " + request.username()));

        return generateAuthResponse(user);
    }

    /** Registers a new user. */
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        // Validate user doesn't exist
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        // Get default role
        Role defaultRole =
                roleRepository
                        .findByName(DEFAULT_ROLE)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Default role not found: " + DEFAULT_ROLE));

        // Create user
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
     * Refreshes the access token using a valid refresh token.
     *
     * <p>Implements refresh token rotation: the used token is invalidated and a new one is issued
     * along with the access token.
     */
    @Transactional
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String refreshToken = request.refreshToken();

        // Validate it's a refresh token
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new ValidationException("Invalid token: not a refresh token");
        }

        // Validate token structure
        if (!jwtService.isTokenStructureValid(refreshToken)) {
            throw new ValidationException("Invalid or expired refresh token");
        }

        // Verify token is not blacklisted
        String tokenId = jwtService.extractTokenId(refreshToken);
        if (tokenBlacklistService.isBlacklisted(tokenId)) {
            throw new ValidationException("Refresh token already used or revoked");
        }

        // Extract username from token
        String username = jwtService.extractUsername(refreshToken);
        User user =
                userRepository
                        .findActiveByUsername(username)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("User not found: " + username));

        // ROTATION: Invalidate the used refresh token (single-use)
        tokenBlacklistService.blacklistToken(
                tokenId,
                username,
                jwtService.extractExpiration(refreshToken),
                BlacklistReason.TOKEN_ROTATED);

        // Generate new tokens (access + refresh)
        return generateAuthResponse(user);
    }

    /** Generates the authentication response with tokens. */
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
     * Invalidates a token by adding it to the blacklist.
     *
     * @param token JWT token to invalidate
     */
    @Transactional
    public void logout(String token) {
        String tokenId = jwtService.extractTokenId(token);
        String username = jwtService.extractUsername(token);
        Instant expiration = jwtService.extractExpiration(token);

        tokenBlacklistService.blacklistToken(tokenId, username, expiration, BlacklistReason.LOGOUT);
    }

    /**
     * Invalidates all tokens for a user. Useful when the user changes their password.
     *
     * @param username Username
     */
    @Transactional
    public void revokeAllUserTokens(String username) {
        tokenBlacklistService.revokeAllUserTokens(username);
    }
}
