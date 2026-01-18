package com.jnzader.apigen.core.infrastructure.config;

import com.jnzader.apigen.core.domain.entity.audit.Revision;
import org.hibernate.envers.RevisionListener;

/**
 * Listener personalizado para Hibernate Envers que permite capturar
 * información adicional en la entidad de revisión, como el usuario que
 * realiza la modificación.
 * <p>
 * Este listener usa reflection para acceder a Spring Security, de forma
 * que funciona tanto con como sin Spring Security en el classpath.
 */
public class CustomRevisionListener implements RevisionListener {

    private static final String USERNAME_SYSTEM = "system";
    private static final String USERNAME_ANONYMOUS = "anonymous";
    private static final boolean SECURITY_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            available = true;
        } catch (ClassNotFoundException _) {
            // Spring Security no está disponible
        }
        SECURITY_AVAILABLE = available;
    }

    @Override
    public void newRevision(Object revisionEntity) {
        Revision revision = (Revision) revisionEntity;
        String username = getCurrentUsername();
        revision.setUsername(username);
    }

    private String getCurrentUsername() {
        if (!SECURITY_AVAILABLE) {
            return USERNAME_SYSTEM;
        }

        try {
            // Usar reflection para acceder a Spring Security
            Class<?> securityContextHolderClass = Class.forName(
                    "org.springframework.security.core.context.SecurityContextHolder");
            Object securityContext = securityContextHolderClass.getMethod("getContext").invoke(null);

            if (securityContext == null) {
                return USERNAME_SYSTEM;
            }

            Object authentication = securityContext.getClass().getMethod("getAuthentication").invoke(securityContext);

            if (authentication == null) {
                return USERNAME_ANONYMOUS;
            }

            boolean isAuthenticated = (boolean) authentication.getClass()
                    .getMethod("isAuthenticated").invoke(authentication);

            if (!isAuthenticated) {
                return USERNAME_ANONYMOUS;
            }

            String name = (String) authentication.getClass().getMethod("getName").invoke(authentication);

            if ("anonymousUser".equals(name)) {
                return USERNAME_ANONYMOUS;
            }

            return name;
        } catch (Exception _) {
            // Si hay cualquier error con reflection, usar "system"
            return USERNAME_SYSTEM;
        }
    }
}
