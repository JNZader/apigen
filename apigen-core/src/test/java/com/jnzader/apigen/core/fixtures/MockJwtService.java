package com.jnzader.apigen.core.fixtures;

/**
 * Mock JwtService interface for tests.
 * This avoids dependency on the security module.
 */
public interface MockJwtService {
    String extractUsername(String token);
    String generateToken(String username);
    boolean isTokenValid(String token, String username);
}
