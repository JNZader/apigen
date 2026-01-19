package com.jnzader.apigen.security.application.service;

import com.jnzader.apigen.security.domain.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación personalizada de UserDetailsService.
 *
 * <p>Carga usuarios desde la base de datos para la autenticación.
 */
@Service
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findActiveByUsername(username)
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "Usuario no encontrado o inactivo: " + username));
    }
}
