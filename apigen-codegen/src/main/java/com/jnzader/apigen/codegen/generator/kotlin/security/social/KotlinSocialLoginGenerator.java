package com.jnzader.apigen.codegen.generator.kotlin.security.social;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Kotlin social login functionality with OAuth2 providers.
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
public class KotlinSocialLoginGenerator {

    private static final String PKG_SECURITY = "security";

    private final String basePackage;

    public KotlinSocialLoginGenerator(String basePackage) {
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
        String basePath = "src/main/kotlin/" + basePackage.replace('.', '/');

        // Generate Security Config
        files.put(
                basePath + "/" + PKG_SECURITY + "/config/OAuth2SecurityConfig.kt",
                generateSecurityConfig());

        // Generate OAuth2 Success Handler
        files.put(
                basePath + "/" + PKG_SECURITY + "/handler/OAuth2AuthenticationSuccessHandler.kt",
                generateSuccessHandler());

        // Generate OAuth2 Failure Handler
        files.put(
                basePath + "/" + PKG_SECURITY + "/handler/OAuth2AuthenticationFailureHandler.kt",
                generateFailureHandler());

        // Generate SocialUserService
        files.put(
                basePath + "/" + PKG_SECURITY + "/service/SocialUserService.kt",
                generateSocialUserService(autoCreateUser, linkByEmail));

        // Generate SocialAccount Entity
        files.put(
                basePath + "/" + PKG_SECURITY + "/entity/SocialAccount.kt",
                generateSocialAccountEntity());

        // Generate SocialAccountRepository
        files.put(
                basePath + "/" + PKG_SECURITY + "/repository/SocialAccountRepository.kt",
                generateSocialAccountRepository());

        // Generate OAuth2UserInfo classes
        files.put(
                basePath + "/" + PKG_SECURITY + "/oauth2/OAuth2UserInfo.kt",
                generateOAuth2UserInfo());

        files.put(
                basePath + "/" + PKG_SECURITY + "/oauth2/OAuth2UserInfoFactory.kt",
                generateOAuth2UserInfoFactory(enableGoogle, enableGithub, enableLinkedin));

        if (enableGoogle) {
            files.put(
                    basePath + "/" + PKG_SECURITY + "/oauth2/GoogleOAuth2UserInfo.kt",
                    generateGoogleUserInfo());
        }

        if (enableGithub) {
            files.put(
                    basePath + "/" + PKG_SECURITY + "/oauth2/GithubOAuth2UserInfo.kt",
                    generateGithubUserInfo());
        }

        if (enableLinkedin) {
            files.put(
                    basePath + "/" + PKG_SECURITY + "/oauth2/LinkedinOAuth2UserInfo.kt",
                    generateLinkedinUserInfo());
        }

        // Generate CustomOAuth2UserService
        files.put(
                basePath + "/" + PKG_SECURITY + "/service/CustomOAuth2UserService.kt",
                generateCustomOAuth2UserService());

        // Generate OAuth2AuthenticationException
        files.put(
                basePath + "/" + PKG_SECURITY + "/exception/OAuth2AuthenticationException.kt",
                generateOAuth2Exception());

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
        package %s.security.config

        import %s.security.handler.OAuth2AuthenticationFailureHandler
        import %s.security.handler.OAuth2AuthenticationSuccessHandler
        import %s.security.service.CustomOAuth2UserService
        import org.springframework.context.annotation.Bean
        import org.springframework.context.annotation.Configuration
        import org.springframework.security.config.annotation.web.builders.HttpSecurity
        import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
        import org.springframework.security.web.SecurityFilterChain

        /**
         * Security configuration for OAuth2 social login.
         */
        @Configuration
        @EnableWebSecurity
        class OAuth2SecurityConfig(
            private val customOAuth2UserService: CustomOAuth2UserService,
            private val successHandler: OAuth2AuthenticationSuccessHandler,
            private val failureHandler: OAuth2AuthenticationFailureHandler
        ) {

            @Bean
            fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
                http
                    .authorizeHttpRequests { auth ->
                        auth
                            .requestMatchers("/", "/login", "/error", "/oauth2/**").permitAll()
                            .requestMatchers("/api/public/**").permitAll()
                            .anyRequest().authenticated()
                    }
                    .oauth2Login { oauth2 ->
                        oauth2
                            .userInfoEndpoint { userInfo ->
                                userInfo.userService(customOAuth2UserService)
                            }
                            .successHandler(successHandler)
                            .failureHandler(failureHandler)
                    }
                    .logout { logout ->
                        logout
                            .logoutSuccessUrl("/")
                            .permitAll()
                    }

                return http.build()
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage);
    }

    private String generateSuccessHandler() {
        return """
        package %s.security.handler

        import jakarta.servlet.http.HttpServletRequest
        import jakarta.servlet.http.HttpServletResponse
        import org.slf4j.LoggerFactory
        import org.springframework.beans.factory.annotation.Value
        import org.springframework.security.core.Authentication
        import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
        import org.springframework.stereotype.Component

        /**
         * Handler for successful OAuth2 authentication.
         */
        @Component
        class OAuth2AuthenticationSuccessHandler : SimpleUrlAuthenticationSuccessHandler() {

            private val log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler::class.java)

            @Value("\\${app.oauth2.success-redirect-url:/}")
            private lateinit var successRedirectUrl: String

            override fun onAuthenticationSuccess(
                request: HttpServletRequest,
                response: HttpServletResponse,
                authentication: Authentication
            ) {
                log.info("OAuth2 authentication successful for user: {}", authentication.name)

                // You can add JWT token generation here if needed
                // val token = jwtService.generateToken(authentication)
                // response.addHeader("Authorization", "Bearer $token")

                redirectStrategy.sendRedirect(request, response, successRedirectUrl)
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateFailureHandler() {
        return """
        package %s.security.handler

        import jakarta.servlet.http.HttpServletRequest
        import jakarta.servlet.http.HttpServletResponse
        import org.slf4j.LoggerFactory
        import org.springframework.beans.factory.annotation.Value
        import org.springframework.security.core.AuthenticationException
        import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
        import org.springframework.stereotype.Component
        import org.springframework.web.util.UriComponentsBuilder

        /**
         * Handler for failed OAuth2 authentication.
         */
        @Component
        class OAuth2AuthenticationFailureHandler : SimpleUrlAuthenticationFailureHandler() {

            private val log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler::class.java)

            @Value("\\${app.oauth2.failure-redirect-url:/login?error}")
            private lateinit var failureRedirectUrl: String

            override fun onAuthenticationFailure(
                request: HttpServletRequest,
                response: HttpServletResponse,
                exception: AuthenticationException
            ) {
                log.error("OAuth2 authentication failed: {}", exception.message)

                val targetUrl = UriComponentsBuilder.fromUriString(failureRedirectUrl)
                    .queryParam("error", exception.localizedMessage)
                    .build().toUriString()

                redirectStrategy.sendRedirect(request, response, targetUrl)
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateSocialUserService(boolean autoCreateUser, boolean linkByEmail) {
        return """
        package %s.security.service

        import %s.security.entity.SocialAccount
        import %s.security.exception.OAuth2AuthenticationException
        import %s.security.oauth2.OAuth2UserInfo
        import %s.security.repository.SocialAccountRepository
        import org.slf4j.LoggerFactory
        import org.springframework.stereotype.Service
        import org.springframework.transaction.annotation.Transactional
        import java.time.LocalDateTime

        /**
         * Service for managing social login users.
         */
        @Service
        class SocialUserService(
            private val socialAccountRepository: SocialAccountRepository
        ) {

            private val log = LoggerFactory.getLogger(SocialUserService::class.java)

            companion object {
                private const val AUTO_CREATE_USER = %s
                private const val LINK_BY_EMAIL = %s
            }

            /**
             * Processes OAuth2 login - creates or updates social account.
             *
             * @param registrationId OAuth2 provider ID (google, github, linkedin)
             * @param userInfo OAuth2 user information
             * @return the social account
             */
            @Transactional
            fun processOAuth2Login(registrationId: String, userInfo: OAuth2UserInfo): SocialAccount {
                val existingAccount = socialAccountRepository
                    .findByProviderAndProviderId(registrationId, userInfo.id)

                if (existingAccount.isPresent) {
                    return updateExistingAccount(existingAccount.get(), userInfo)
                }

                // Check if user exists by email and link accounts
                if (LINK_BY_EMAIL && userInfo.email != null) {
                    val accountByEmail = socialAccountRepository.findByEmail(userInfo.email!!)
                    if (accountByEmail.isPresent) {
                        log.info(
                            "Linking {} account to existing user with email: {}",
                            registrationId, userInfo.email
                        )
                        // Create new social account linked to existing user
                        return createNewSocialAccount(
                            registrationId, userInfo, accountByEmail.get().userId
                        )
                    }
                }

                if (AUTO_CREATE_USER) {
                    return createNewSocialAccount(registrationId, userInfo, null)
                }

                throw OAuth2AuthenticationException(
                    "User with email ${'$'}{userInfo.email} not found and auto-registration is disabled"
                )
            }

            private fun updateExistingAccount(
                account: SocialAccount,
                userInfo: OAuth2UserInfo
            ): SocialAccount {
                return socialAccountRepository.save(
                    account.copy(
                        name = userInfo.name,
                        email = userInfo.email,
                        imageUrl = userInfo.imageUrl,
                        lastLoginAt = LocalDateTime.now()
                    )
                )
            }

            private fun createNewSocialAccount(
                provider: String,
                userInfo: OAuth2UserInfo,
                existingUserId: Long?
            ): SocialAccount {
                val account = SocialAccount(
                    provider = provider,
                    providerId = userInfo.id,
                    email = userInfo.email,
                    name = userInfo.name,
                    imageUrl = userInfo.imageUrl,
                    userId = existingUserId,
                    createdAt = LocalDateTime.now(),
                    lastLoginAt = LocalDateTime.now()
                )

                log.info(
                    "Created new social account for {} user: {}",
                    provider, userInfo.email
                )

                return socialAccountRepository.save(account)
            }
        }
        """
                .formatted(
                        basePackage,
                        basePackage,
                        basePackage,
                        basePackage,
                        basePackage,
                        autoCreateUser,
                        linkByEmail);
    }

    private String generateSocialAccountEntity() {
        return """
        package %s.security.entity

        import jakarta.persistence.Column
        import jakarta.persistence.Entity
        import jakarta.persistence.GeneratedValue
        import jakarta.persistence.GenerationType
        import jakarta.persistence.Id
        import jakarta.persistence.Index
        import jakarta.persistence.Table
        import jakarta.persistence.UniqueConstraint
        import java.time.LocalDateTime

        /**
         * Entity for storing OAuth2 social accounts.
         */
        @Entity
        @Table(
            name = "social_accounts",
            indexes = [
                Index(name = "idx_sa_email", columnList = "email"),
                Index(name = "idx_sa_user_id", columnList = "user_id")
            ],
            uniqueConstraints = [
                UniqueConstraint(
                    name = "uk_sa_provider_id",
                    columnNames = ["provider", "provider_id"]
                )
            ]
        )
        data class SocialAccount(
            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            val id: Long? = null,

            @Column(nullable = false, length = 20)
            val provider: String,

            @Column(name = "provider_id", nullable = false, length = 100)
            val providerId: String,

            @Column(length = 320)
            val email: String? = null,

            @Column(length = 100)
            val name: String? = null,

            @Column(name = "image_url", length = 2048)
            val imageUrl: String? = null,

            @Column(name = "user_id")
            val userId: Long? = null,

            @Column(name = "created_at", nullable = false)
            val createdAt: LocalDateTime = LocalDateTime.now(),

            @Column(name = "last_login_at")
            val lastLoginAt: LocalDateTime? = null
        )
        """
                .formatted(basePackage);
    }

    private String generateSocialAccountRepository() {
        return """
        package %s.security.repository

        import %s.security.entity.SocialAccount
        import org.springframework.data.jpa.repository.JpaRepository
        import org.springframework.stereotype.Repository
        import java.util.Optional

        /**
         * Repository for social account operations.
         */
        @Repository
        interface SocialAccountRepository : JpaRepository<SocialAccount, Long> {

            fun findByProviderAndProviderId(provider: String, providerId: String): Optional<SocialAccount>

            fun findByEmail(email: String): Optional<SocialAccount>

            fun findByUserId(userId: Long): Optional<SocialAccount>
        }
        """
                .formatted(basePackage, basePackage);
    }

    private String generateOAuth2UserInfo() {
        return """
        package %s.security.oauth2

        /**
         * Abstract class for extracting user info from OAuth2 providers.
         */
        abstract class OAuth2UserInfo(
            protected val attributes: Map<String, Any>
        ) {
            abstract val id: String
            abstract val name: String?
            abstract val email: String?
            abstract val imageUrl: String?

            fun getAttributes(): Map<String, Any> = attributes
        }
        """
                .formatted(basePackage);
    }

    private String generateOAuth2UserInfoFactory(boolean google, boolean github, boolean linkedin) {
        StringBuilder providers = new StringBuilder();

        if (google) {
            providers.append(
                    """
                            "google" -> GoogleOAuth2UserInfo(attributes)
                    """);
        }
        if (github) {
            providers.append(
                    """
                            "github" -> GithubOAuth2UserInfo(attributes)
                    """);
        }
        if (linkedin) {
            providers.append(
                    """
                            "linkedin" -> LinkedinOAuth2UserInfo(attributes)
                    """);
        }

        return """
        package %s.security.oauth2

        /**
         * Factory for creating OAuth2UserInfo instances based on provider.
         */
        object OAuth2UserInfoFactory {

            fun getOAuth2UserInfo(
                registrationId: String,
                attributes: Map<String, Any>
            ): OAuth2UserInfo {
                return when (registrationId.lowercase()) {
        %s
                    else -> throw IllegalArgumentException(
                        "Unsupported OAuth2 provider: $registrationId"
                    )
                }
            }
        }
        """
                .formatted(basePackage, providers);
    }

    private String generateGoogleUserInfo() {
        return """
        package %s.security.oauth2

        /**
         * Google OAuth2 user info extractor.
         */
        class GoogleOAuth2UserInfo(attributes: Map<String, Any>) : OAuth2UserInfo(attributes) {

            override val id: String
                get() = attributes["sub"] as String

            override val name: String?
                get() = attributes["name"] as? String

            override val email: String?
                get() = attributes["email"] as? String

            override val imageUrl: String?
                get() = attributes["picture"] as? String
        }
        """
                .formatted(basePackage);
    }

    private String generateGithubUserInfo() {
        return """
        package %s.security.oauth2

        /**
         * GitHub OAuth2 user info extractor.
         */
        class GithubOAuth2UserInfo(attributes: Map<String, Any>) : OAuth2UserInfo(attributes) {

            override val id: String
                get() = attributes["id"].toString()

            override val name: String?
                get() = (attributes["name"] as? String) ?: (attributes["login"] as? String)

            override val email: String?
                get() = attributes["email"] as? String

            override val imageUrl: String?
                get() = attributes["avatar_url"] as? String
        }
        """
                .formatted(basePackage);
    }

    private String generateLinkedinUserInfo() {
        return """
        package %s.security.oauth2

        /**
         * LinkedIn OAuth2 user info extractor.
         */
        class LinkedinOAuth2UserInfo(attributes: Map<String, Any>) : OAuth2UserInfo(attributes) {

            override val id: String
                get() = attributes["id"] as String

            override val name: String?
                get() {
                    val firstName = attributes["localizedFirstName"] as? String ?: ""
                    val lastName = attributes["localizedLastName"] as? String ?: ""
                    return "$firstName $lastName".trim().ifEmpty { null }
                }

            override val email: String?
                get() = attributes["emailAddress"] as? String

            override val imageUrl: String?
                get() = attributes["pictureUrl"] as? String
        }
        """
                .formatted(basePackage);
    }

    private String generateCustomOAuth2UserService() {
        return """
        package %s.security.service

        import %s.security.oauth2.OAuth2UserInfoFactory
        import org.springframework.security.core.authority.SimpleGrantedAuthority
        import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
        import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
        import org.springframework.security.oauth2.core.OAuth2AuthenticationException
        import org.springframework.security.oauth2.core.user.DefaultOAuth2User
        import org.springframework.security.oauth2.core.user.OAuth2User
        import org.springframework.stereotype.Service

        /**
         * Custom OAuth2 user service for processing OAuth2 logins.
         */
        @Service
        class CustomOAuth2UserService(
            private val socialUserService: SocialUserService
        ) : DefaultOAuth2UserService() {

            override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
                val oAuth2User = super.loadUser(userRequest)

                val registrationId = userRequest.clientRegistration.registrationId
                val userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                    registrationId, oAuth2User.attributes
                )

                if (userInfo.email.isNullOrEmpty()) {
                    throw OAuth2AuthenticationException("Email not found from OAuth2 provider")
                }

                try {
                    socialUserService.processOAuth2Login(registrationId, userInfo)

                    return DefaultOAuth2User(
                        listOf(SimpleGrantedAuthority("ROLE_USER")),
                        oAuth2User.attributes,
                        userRequest.clientRegistration
                            .providerDetails
                            .userInfoEndpoint
                            .userNameAttributeName
                    )
                } catch (e: Exception) {
                    throw OAuth2AuthenticationException(e.message)
                }
            }
        }
        """
                .formatted(basePackage, basePackage);
    }

    private String generateOAuth2Exception() {
        return """
        package %s.security.exception

        /**
         * Exception thrown when OAuth2 authentication fails.
         */
        class OAuth2AuthenticationException(message: String) : RuntimeException(message)
        """
                .formatted(basePackage);
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
