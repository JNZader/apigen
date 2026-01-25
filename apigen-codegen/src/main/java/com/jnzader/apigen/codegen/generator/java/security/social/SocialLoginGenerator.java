package com.jnzader.apigen.codegen.generator.java.security.social;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates social login functionality with OAuth2 providers.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>OAuth2 Security configuration
 *   <li>OAuth2 success/failure handlers
 *   <li>SocialUserService for user creation/linking
 *   <li>SocialAccount entity for storing OAuth2 accounts
 *   <li>Configuration properties
 * </ul>
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: template strings; S3400: template methods return constants
public class SocialLoginGenerator {

    private static final String PKG_SECURITY = "security";

    private final String basePackage;

    public SocialLoginGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Generates all social login files.
     *
     * @param enableGoogle enable Google OAuth2
     * @param enableGithub enable GitHub OAuth2
     * @param enableLinkedin enable LinkedIn OAuth2
     * @param autoCreateUser auto-create user on first login
     * @param linkByEmail link social account to existing user by email
     * @return map of file path to content
     */
    public Map<String, String> generate(
            boolean enableGoogle,
            boolean enableGithub,
            boolean enableLinkedin,
            boolean autoCreateUser,
            boolean linkByEmail) {

        Map<String, String> files = new LinkedHashMap<>();
        String basePath = "src/main/java/" + basePackage.replace('.', '/');

        // Generate Security Config
        files.put(
                basePath + "/" + PKG_SECURITY + "/config/OAuth2SecurityConfig.java",
                generateSecurityConfig());

        // Generate OAuth2 Success Handler
        files.put(
                basePath + "/" + PKG_SECURITY + "/handler/OAuth2AuthenticationSuccessHandler.java",
                generateSuccessHandler());

        // Generate OAuth2 Failure Handler
        files.put(
                basePath + "/" + PKG_SECURITY + "/handler/OAuth2AuthenticationFailureHandler.java",
                generateFailureHandler());

        // Generate SocialUserService
        files.put(
                basePath + "/" + PKG_SECURITY + "/service/SocialUserService.java",
                generateSocialUserService(autoCreateUser, linkByEmail));

        // Generate SocialAccount Entity
        files.put(
                basePath + "/" + PKG_SECURITY + "/entity/SocialAccount.java",
                generateSocialAccountEntity());

        // Generate SocialAccountRepository
        files.put(
                basePath + "/" + PKG_SECURITY + "/repository/SocialAccountRepository.java",
                generateSocialAccountRepository());

        // Generate OAuth2UserInfo classes
        files.put(
                basePath + "/" + PKG_SECURITY + "/oauth2/OAuth2UserInfo.java",
                generateOAuth2UserInfo());

        files.put(
                basePath + "/" + PKG_SECURITY + "/oauth2/OAuth2UserInfoFactory.java",
                generateOAuth2UserInfoFactory(enableGoogle, enableGithub, enableLinkedin));

        if (enableGoogle) {
            files.put(
                    basePath + "/" + PKG_SECURITY + "/oauth2/GoogleOAuth2UserInfo.java",
                    generateGoogleUserInfo());
        }

        if (enableGithub) {
            files.put(
                    basePath + "/" + PKG_SECURITY + "/oauth2/GithubOAuth2UserInfo.java",
                    generateGithubUserInfo());
        }

        if (enableLinkedin) {
            files.put(
                    basePath + "/" + PKG_SECURITY + "/oauth2/LinkedinOAuth2UserInfo.java",
                    generateLinkedinUserInfo());
        }

        // Generate CustomOAuth2UserService
        files.put(
                basePath + "/" + PKG_SECURITY + "/service/CustomOAuth2UserService.java",
                generateCustomOAuth2UserService());

        // Generate application-oauth2.yml
        files.put(
                "src/main/resources/application-oauth2.yml",
                generateApplicationConfig(enableGoogle, enableGithub, enableLinkedin));

        // Generate Migration
        files.put(
                "src/main/resources/db/migration/V997__create_social_accounts_table.sql",
                generateMigration());

        return files;
    }

    private String generateSecurityConfig() {
        return """
        package %s.security.config;

        import %s.security.handler.OAuth2AuthenticationFailureHandler;
        import %s.security.handler.OAuth2AuthenticationSuccessHandler;
        import %s.security.service.CustomOAuth2UserService;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.security.config.annotation.web.builders.HttpSecurity;
        import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
        import org.springframework.security.web.SecurityFilterChain;

        /**
         * Security configuration for OAuth2 social login.
         */
        @Configuration
        @EnableWebSecurity
        public class OAuth2SecurityConfig {

            private final CustomOAuth2UserService customOAuth2UserService;
            private final OAuth2AuthenticationSuccessHandler successHandler;
            private final OAuth2AuthenticationFailureHandler failureHandler;

            public OAuth2SecurityConfig(
                    CustomOAuth2UserService customOAuth2UserService,
                    OAuth2AuthenticationSuccessHandler successHandler,
                    OAuth2AuthenticationFailureHandler failureHandler) {
                this.customOAuth2UserService = customOAuth2UserService;
                this.successHandler = successHandler;
                this.failureHandler = failureHandler;
            }

            @Bean
            public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                    .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/error", "/oauth2/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .anyRequest().authenticated()
                    )
                    .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                        )
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                    )
                    .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                    );

                return http.build();
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String generateSuccessHandler() {
        return """
        package %s.security.handler;

        import jakarta.servlet.ServletException;
        import jakarta.servlet.http.HttpServletRequest;
        import jakarta.servlet.http.HttpServletResponse;
        import java.io.IOException;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.security.core.Authentication;
        import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
        import org.springframework.stereotype.Component;

        /**
         * Handler for successful OAuth2 authentication.
         */
        @Component
        public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

            private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

            @Value("${app.oauth2.success-redirect-url:/}")
            private String successRedirectUrl;

            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {

                log.info("OAuth2 authentication successful for user: {}", authentication.getName());

                // You can add JWT token generation here if needed
                // String token = jwtService.generateToken(authentication);
                // response.addHeader("Authorization", "Bearer " + token);

                getRedirectStrategy().sendRedirect(request, response, successRedirectUrl);
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateFailureHandler() {
        return """
        package %s.security.handler;

        import jakarta.servlet.ServletException;
        import jakarta.servlet.http.HttpServletRequest;
        import jakarta.servlet.http.HttpServletResponse;
        import java.io.IOException;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.security.core.AuthenticationException;
        import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
        import org.springframework.stereotype.Component;
        import org.springframework.web.util.UriComponentsBuilder;

        /**
         * Handler for failed OAuth2 authentication.
         */
        @Component
        public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

            private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

            @Value("${app.oauth2.failure-redirect-url:/login?error}")
            private String failureRedirectUrl;

            @Override
            public void onAuthenticationFailure(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    AuthenticationException exception) throws IOException, ServletException {

                log.error("OAuth2 authentication failed: {}", exception.getMessage());

                String targetUrl = UriComponentsBuilder.fromUriString(failureRedirectUrl)
                        .queryParam("error", exception.getLocalizedMessage())
                        .build().toUriString();

                getRedirectStrategy().sendRedirect(request, response, targetUrl);
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateSocialUserService(boolean autoCreateUser, boolean linkByEmail) {
        return """
        package %s.security.service;

        import %s.security.entity.SocialAccount;
        import %s.security.oauth2.OAuth2UserInfo;
        import %s.security.repository.SocialAccountRepository;
        import java.time.LocalDateTime;
        import java.util.Optional;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.stereotype.Service;
        import org.springframework.transaction.annotation.Transactional;

        /**
         * Service for managing social login users.
         */
        @Service
        public class SocialUserService {

            private static final Logger log = LoggerFactory.getLogger(SocialUserService.class);
            private static final boolean AUTO_CREATE_USER = %s;
            private static final boolean LINK_BY_EMAIL = %s;

            private final SocialAccountRepository socialAccountRepository;

            public SocialUserService(SocialAccountRepository socialAccountRepository) {
                this.socialAccountRepository = socialAccountRepository;
            }

            /**
             * Processes OAuth2 login - creates or updates social account.
             *
             * @param registrationId OAuth2 provider ID (google, github, linkedin)
             * @param userInfo OAuth2 user information
             * @return the social account
             */
            @Transactional
            public SocialAccount processOAuth2Login(String registrationId, OAuth2UserInfo userInfo) {
                Optional<SocialAccount> existingAccount = socialAccountRepository
                        .findByProviderAndProviderId(registrationId, userInfo.getId());

                if (existingAccount.isPresent()) {
                    return updateExistingAccount(existingAccount.get(), userInfo);
                }

                // Check if user exists by email and link accounts
                if (LINK_BY_EMAIL && userInfo.getEmail() != null) {
                    Optional<SocialAccount> accountByEmail = socialAccountRepository
                            .findByEmail(userInfo.getEmail());
                    if (accountByEmail.isPresent()) {
                        log.info("Linking {} account to existing user with email: {}",
                                registrationId, userInfo.getEmail());
                        // Create new social account linked to existing user
                        return createNewSocialAccount(registrationId, userInfo,
                                accountByEmail.get().getUserId());
                    }
                }

                if (AUTO_CREATE_USER) {
                    return createNewSocialAccount(registrationId, userInfo, null);
                }

                throw new OAuth2AuthenticationException(
                        "User with email " + userInfo.getEmail() + " not found and auto-registration is disabled");
            }

            private SocialAccount updateExistingAccount(SocialAccount account, OAuth2UserInfo userInfo) {
                account.setName(userInfo.getName());
                account.setEmail(userInfo.getEmail());
                account.setImageUrl(userInfo.getImageUrl());
                account.setLastLoginAt(LocalDateTime.now());
                return socialAccountRepository.save(account);
            }

            private SocialAccount createNewSocialAccount(
                    String provider, OAuth2UserInfo userInfo, Long existingUserId) {

                SocialAccount account = SocialAccount.builder()
                        .provider(provider)
                        .providerId(userInfo.getId())
                        .email(userInfo.getEmail())
                        .name(userInfo.getName())
                        .imageUrl(userInfo.getImageUrl())
                        .userId(existingUserId)
                        .createdAt(LocalDateTime.now())
                        .lastLoginAt(LocalDateTime.now())
                        .build();

                log.info("Created new social account for {} user: {}",
                        provider, userInfo.getEmail());

                return socialAccountRepository.save(account);
            }

            /**
             * Exception thrown when OAuth2 authentication fails.
             */
            public static class OAuth2AuthenticationException extends RuntimeException {
                public OAuth2AuthenticationException(String message) {
                    super(message);
                }
            }
        }
        """
                .formatted(
                        basePackage,
                        basePackage,
                        basePackage,
                        basePackage,
                        autoCreateUser,
                        linkByEmail);
    }

    private String generateSocialAccountEntity() {
        return """
        package %s.security.entity;

        import jakarta.persistence.Column;
        import jakarta.persistence.Entity;
        import jakarta.persistence.GeneratedValue;
        import jakarta.persistence.GenerationType;
        import jakarta.persistence.Id;
        import jakarta.persistence.Index;
        import jakarta.persistence.Table;
        import jakarta.persistence.UniqueConstraint;
        import java.time.LocalDateTime;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        /**
         * Entity for storing OAuth2 social accounts.
         */
        @Entity
        @Table(
                name = "social_accounts",
                indexes = {
                    @Index(name = "idx_sa_email", columnList = "email"),
                    @Index(name = "idx_sa_user_id", columnList = "user_id")
                },
                uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_sa_provider_id",
                            columnNames = {"provider", "provider_id"})
                })
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class SocialAccount {

            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            private Long id;

            @Column(nullable = false, length = 20)
            private String provider;

            @Column(name = "provider_id", nullable = false, length = 100)
            private String providerId;

            @Column(length = 320)
            private String email;

            @Column(length = 100)
            private String name;

            @Column(name = "image_url", length = 2048)
            private String imageUrl;

            @Column(name = "user_id")
            private Long userId;

            @Column(name = "created_at", nullable = false)
            private LocalDateTime createdAt;

            @Column(name = "last_login_at")
            private LocalDateTime lastLoginAt;
        }
        """
                .formatted(basePackage);
    }

    private String generateSocialAccountRepository() {
        return """
        package %s.security.repository;

        import %s.security.entity.SocialAccount;
        import java.util.Optional;
        import org.springframework.data.jpa.repository.JpaRepository;
        import org.springframework.stereotype.Repository;

        /**
         * Repository for social account operations.
         */
        @Repository
        public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

            Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);

            Optional<SocialAccount> findByEmail(String email);

            Optional<SocialAccount> findByUserId(Long userId);
        }
        """
                .formatted(basePackage, basePackage);
    }

    private String generateOAuth2UserInfo() {
        return """
        package %s.security.oauth2;

        import java.util.Map;

        /**
         * Abstract class for extracting user info from OAuth2 providers.
         */
        public abstract class OAuth2UserInfo {

            protected Map<String, Object> attributes;

            protected OAuth2UserInfo(Map<String, Object> attributes) {
                this.attributes = attributes;
            }

            public Map<String, Object> getAttributes() {
                return attributes;
            }

            public abstract String getId();

            public abstract String getName();

            public abstract String getEmail();

            public abstract String getImageUrl();
        }
        """
                .formatted(basePackage);
    }

    private String generateOAuth2UserInfoFactory(boolean google, boolean github, boolean linkedin) {
        StringBuilder providers = new StringBuilder();

        if (google) {
            providers.append(
                    """
                                case "google" -> new GoogleOAuth2UserInfo(attributes);
                    """);
        }
        if (github) {
            providers.append(
                    """
                                case "github" -> new GithubOAuth2UserInfo(attributes);
                    """);
        }
        if (linkedin) {
            providers.append(
                    """
                                case "linkedin" -> new LinkedinOAuth2UserInfo(attributes);
                    """);
        }

        return """
        package %s.security.oauth2;

        import java.util.Map;

        /**
         * Factory for creating OAuth2UserInfo instances based on provider.
         */
        public class OAuth2UserInfoFactory {

            private OAuth2UserInfoFactory() {}

            public static OAuth2UserInfo getOAuth2UserInfo(
                    String registrationId, Map<String, Object> attributes) {
                return switch (registrationId.toLowerCase()) {
        %s
                    default -> throw new IllegalArgumentException(
                            "Unsupported OAuth2 provider: " + registrationId);
                };
            }
        }
        """
                .formatted(basePackage, providers);
    }

    private String generateGoogleUserInfo() {
        return """
        package %s.security.oauth2;

        import java.util.Map;

        /**
         * Google OAuth2 user info extractor.
         */
        public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

            public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
                super(attributes);
            }

            @Override
            public String getId() {
                return (String) attributes.get("sub");
            }

            @Override
            public String getName() {
                return (String) attributes.get("name");
            }

            @Override
            public String getEmail() {
                return (String) attributes.get("email");
            }

            @Override
            public String getImageUrl() {
                return (String) attributes.get("picture");
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateGithubUserInfo() {
        return """
        package %s.security.oauth2;

        import java.util.Map;

        /**
         * GitHub OAuth2 user info extractor.
         */
        public class GithubOAuth2UserInfo extends OAuth2UserInfo {

            public GithubOAuth2UserInfo(Map<String, Object> attributes) {
                super(attributes);
            }

            @Override
            public String getId() {
                return String.valueOf(attributes.get("id"));
            }

            @Override
            public String getName() {
                String name = (String) attributes.get("name");
                return name != null ? name : (String) attributes.get("login");
            }

            @Override
            public String getEmail() {
                return (String) attributes.get("email");
            }

            @Override
            public String getImageUrl() {
                return (String) attributes.get("avatar_url");
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateLinkedinUserInfo() {
        return """
        package %s.security.oauth2;

        import java.util.Map;

        /**
         * LinkedIn OAuth2 user info extractor.
         */
        public class LinkedinOAuth2UserInfo extends OAuth2UserInfo {

            public LinkedinOAuth2UserInfo(Map<String, Object> attributes) {
                super(attributes);
            }

            @Override
            public String getId() {
                return (String) attributes.get("id");
            }

            @Override
            public String getName() {
                String firstName = (String) attributes.get("localizedFirstName");
                String lastName = (String) attributes.get("localizedLastName");
                return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
            }

            @Override
            public String getEmail() {
                return (String) attributes.get("emailAddress");
            }

            @Override
            public String getImageUrl() {
                return (String) attributes.get("pictureUrl");
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateCustomOAuth2UserService() {
        return """
        package %s.security.service;

        import %s.security.entity.SocialAccount;
        import %s.security.oauth2.OAuth2UserInfo;
        import %s.security.oauth2.OAuth2UserInfoFactory;
        import java.util.Collections;
        import org.springframework.security.core.authority.SimpleGrantedAuthority;
        import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
        import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
        import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
        import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
        import org.springframework.security.oauth2.core.user.OAuth2User;
        import org.springframework.stereotype.Service;

        /**
         * Custom OAuth2 user service for processing OAuth2 logins.
         */
        @Service
        public class CustomOAuth2UserService extends DefaultOAuth2UserService {

            private final SocialUserService socialUserService;

            public CustomOAuth2UserService(SocialUserService socialUserService) {
                this.socialUserService = socialUserService;
            }

            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
                OAuth2User oAuth2User = super.loadUser(userRequest);

                String registrationId = userRequest.getClientRegistration().getRegistrationId();
                OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                        registrationId, oAuth2User.getAttributes());

                if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
                    throw new OAuth2AuthenticationException(
                            "Email not found from OAuth2 provider");
                }

                try {
                    SocialAccount account = socialUserService.processOAuth2Login(
                            registrationId, userInfo);

                    return new DefaultOAuth2User(
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                            oAuth2User.getAttributes(),
                            userRequest.getClientRegistration()
                                    .getProviderDetails()
                                    .getUserInfoEndpoint()
                                    .getUserNameAttributeName());
                } catch (Exception e) {
                    throw new OAuth2AuthenticationException(e.getMessage());
                }
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String generateApplicationConfig(boolean google, boolean github, boolean linkedin) {
        StringBuilder config = new StringBuilder();
        config.append(
                """
                # OAuth2 Configuration
                spring:
                  security:
                    oauth2:
                      client:
                        registration:
                """);

        if (google) {
            config.append(
                    """
                          google:
                            client-id: ${GOOGLE_CLIENT_ID}
                            client-secret: ${GOOGLE_CLIENT_SECRET}
                            scope:
                              - email
                              - profile
                    """);
        }

        if (github) {
            config.append(
                    """
                          github:
                            client-id: ${GITHUB_CLIENT_ID}
                            client-secret: ${GITHUB_CLIENT_SECRET}
                            scope:
                              - user:email
                              - read:user
                    """);
        }

        if (linkedin) {
            config.append(
                    """
                          linkedin:
                            client-id: ${LINKEDIN_CLIENT_ID}
                            client-secret: ${LINKEDIN_CLIENT_SECRET}
                            authorization-grant-type: authorization_code
                            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
                            scope:
                              - r_liteprofile
                              - r_emailaddress
                        provider:
                          linkedin:
                            authorization-uri: https://www.linkedin.com/oauth/v2/authorization
                            token-uri: https://www.linkedin.com/oauth/v2/accessToken
                            user-info-uri: https://api.linkedin.com/v2/me
                            user-name-attribute: id
                    """);
        }

        config.append(
                """

                # OAuth2 App Configuration
                app:
                  oauth2:
                    success-redirect-url: /
                    failure-redirect-url: /login?error
                """);

        return config.toString();
    }

    private String generateMigration() {
        return """
        -- Social Accounts table
        CREATE TABLE IF NOT EXISTS social_accounts (
            id BIGSERIAL PRIMARY KEY,
            provider VARCHAR(20) NOT NULL,
            provider_id VARCHAR(100) NOT NULL,
            email VARCHAR(320),
            name VARCHAR(100),
            image_url VARCHAR(2048),
            user_id BIGINT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            last_login_at TIMESTAMP,
            CONSTRAINT uk_sa_provider_id UNIQUE (provider, provider_id)
        );

        -- Indexes
        CREATE INDEX IF NOT EXISTS idx_sa_email ON social_accounts(email);
        CREATE INDEX IF NOT EXISTS idx_sa_user_id ON social_accounts(user_id);
        """;
    }
}
