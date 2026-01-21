package com.jnzader.apigen.core.infrastructure.config;

import com.jnzader.apigen.core.domain.entity.audit.Revision;
import org.hibernate.envers.RevisionListener;

/**
 * Custom listener for Hibernate Envers that allows capturing additional information in the revision
 * entity, such as the user performing the modification.
 *
 * <p>This listener uses reflection to access Spring Security, so it works both with and without
 * Spring Security in the classpath.
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
            // Spring Security is not available
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
            // Use reflection to access Spring Security
            Class<?> securityContextHolderClass =
                    Class.forName(
                            "org.springframework.security.core.context.SecurityContextHolder");
            Object securityContext =
                    securityContextHolderClass.getMethod("getContext").invoke(null);

            if (securityContext == null) {
                return USERNAME_SYSTEM;
            }

            Object authentication =
                    securityContext
                            .getClass()
                            .getMethod("getAuthentication")
                            .invoke(securityContext);

            if (authentication == null) {
                return USERNAME_ANONYMOUS;
            }

            boolean isAuthenticated =
                    (boolean)
                            authentication
                                    .getClass()
                                    .getMethod("isAuthenticated")
                                    .invoke(authentication);

            if (!isAuthenticated) {
                return USERNAME_ANONYMOUS;
            }

            String name =
                    (String) authentication.getClass().getMethod("getName").invoke(authentication);

            if ("anonymousUser".equals(name)) {
                return USERNAME_ANONYMOUS;
            }

            return name;
        } catch (Exception _) {
            // If there is any error with reflection, use "system"
            return USERNAME_SYSTEM;
        }
    }
}
