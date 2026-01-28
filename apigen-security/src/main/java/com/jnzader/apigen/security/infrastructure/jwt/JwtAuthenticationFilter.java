package com.jnzader.apigen.security.infrastructure.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter that processes each request.
 *
 * <p>Extracts the token from the Authorization header, validates it, and sets the security context
 * if valid.
 */
@Component
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
@ConditionalOnExpression("'${apigen.security.mode:jwt}'.equalsIgnoreCase('jwt')")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // If no authorization header or not Bearer, continue
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(BEARER_PREFIX.length());
            final String username = jwtService.extractUsername(jwt);

            // If username exists and no prior authentication
            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Verify it's an access token and that it's valid
                if (jwtService.isAccessToken(jwt) && jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("User authenticated: {}", username);
                }
            }
        } catch (ExpiredJwtException e) {
            request.setAttribute("jwt.error", "TOKEN_EXPIRED");
            request.setAttribute("jwt.error.detail", "Token has expired");
            log.debug("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            request.setAttribute("jwt.error", "INVALID_TOKEN");
            request.setAttribute("jwt.error.detail", "Token format is invalid");
            log.debug("Malformed JWT token: {}", e.getMessage());
        } catch (SignatureException e) {
            request.setAttribute("jwt.error", "INVALID_TOKEN");
            request.setAttribute("jwt.error.detail", "Token signature verification failed");
            log.debug("Invalid JWT signature: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            request.setAttribute("jwt.error", "INVALID_TOKEN");
            request.setAttribute("jwt.error.detail", "Token type is not supported");
            log.debug("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            request.setAttribute("jwt.error", "INVALID_TOKEN");
            request.setAttribute("jwt.error.detail", "Token claims are empty");
            log.debug("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            request.setAttribute("jwt.error", "AUTHENTICATION_ERROR");
            request.setAttribute("jwt.error.detail", "Error processing authentication token");
            log.debug("Error processing JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Don't filter authentication endpoints
        return path.startsWith("/api/auth/");
    }
}
