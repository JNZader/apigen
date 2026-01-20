package com.jnzader.apigen.core.infrastructure.i18n;

import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Configuration for internationalization (i18n) support.
 *
 * <p>Configures message source for loading localized messages and locale resolution based on the
 * Accept-Language HTTP header.
 *
 * <p>Supported locales:
 *
 * <ul>
 *   <li>English (en) - Default
 *   <li>Spanish (es)
 * </ul>
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /** Default locale when Accept-Language header is not present or not supported. */
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /**
     * Configures the message source for loading localized messages from properties files.
     *
     * <p>Messages are loaded from:
     *
     * <ul>
     *   <li>classpath:messages.properties (default/English)
     *   <li>classpath:messages_es.properties (Spanish)
     * </ul>
     *
     * @return configured MessageSource bean
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(DEFAULT_LOCALE);
        messageSource.setCacheSeconds(3600); // Reload messages every hour in production
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    /**
     * Configures locale resolution based on the Accept-Language HTTP header.
     *
     * <p>Example headers:
     *
     * <ul>
     *   <li>Accept-Language: es → Spanish
     *   <li>Accept-Language: en-US → English
     *   <li>Accept-Language: fr → Falls back to English (default)
     * </ul>
     *
     * @return configured LocaleResolver bean
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(DEFAULT_LOCALE);
        resolver.setSupportedLocales(
                java.util.List.of(Locale.ENGLISH, Locale.forLanguageTag("es")));
        return resolver;
    }

    /**
     * Creates an interceptor that allows changing locale via a request parameter.
     *
     * <p>This enables locale switching via ?lang=es or ?lang=en query parameter.
     *
     * @return configured LocaleChangeInterceptor
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
