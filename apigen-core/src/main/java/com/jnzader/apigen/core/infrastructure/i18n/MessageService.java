package com.jnzader.apigen.core.infrastructure.i18n;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving localized messages.
 *
 * <p>Provides convenient methods to access internationalized messages based on the current locale
 * determined by the Accept-Language header or default configuration.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @Autowired
 * private MessageService messageService;
 *
 * public void example() {
 *     // Get message for current locale
 *     String title = messageService.getMessage("error.title.not-found");
 *
 *     // Get message with parameters
 *     String detail = messageService.getMessage("error.detail.not-found.with-id", "123");
 *
 *     // Get message for specific locale
 *     String spanish = messageService.getMessage("error.title.not-found", Locale.forLanguageTag("es"));
 * }
 * }</pre>
 */
@Service
public class MessageService {

    private final MessageSource messageSource;

    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Gets a localized message for the current locale.
     *
     * @param code the message code
     * @return the localized message, or the code itself if not found
     */
    public String getMessage(String code) {
        return getMessage(code, (Object[]) null);
    }

    /**
     * Gets a localized message for the current locale with parameters.
     *
     * @param code the message code
     * @param args arguments to be substituted in the message
     * @return the localized message with substituted arguments
     */
    public String getMessage(String code, Object... args) {
        return getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * Gets a localized message for a specific locale.
     *
     * @param code the message code
     * @param locale the target locale
     * @return the localized message for the specified locale
     */
    public String getMessage(String code, Locale locale) {
        return getMessage(code, null, locale);
    }

    /**
     * Gets a localized message for a specific locale with parameters.
     *
     * @param code the message code
     * @param args arguments to be substituted in the message
     * @param locale the target locale
     * @return the localized message with substituted arguments
     */
    public String getMessage(String code, Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, code, locale);
    }

    /**
     * Gets a localized message with a default value if not found.
     *
     * @param code the message code
     * @param defaultMessage the default message if code is not found
     * @return the localized message or the default message
     */
    public String getMessageOrDefault(String code, String defaultMessage) {
        return messageSource.getMessage(
                code, null, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * Gets the current locale from the context.
     *
     * @return the current locale
     */
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    // -------------------------------------------------------------------------
    // Convenience methods for common error messages
    // -------------------------------------------------------------------------

    /** Gets the localized title for "not found" errors. */
    public String getNotFoundTitle() {
        return getMessage("error.title.not-found");
    }

    /** Gets the localized detail for "not found" errors with resource ID. */
    public String getNotFoundDetail(Object resourceId) {
        return getMessage("error.detail.not-found.with-id", resourceId);
    }

    /** Gets the localized title for "conflict" errors. */
    public String getConflictTitle() {
        return getMessage("error.title.conflict");
    }

    /** Gets the localized title for "validation" errors. */
    public String getValidationTitle() {
        return getMessage("error.title.validation");
    }

    /** Gets the localized detail for validation errors with count. */
    public String getValidationDetail(int errorCount) {
        return getMessage("error.detail.validation.count", errorCount);
    }

    /** Gets the localized title for "id mismatch" errors. */
    public String getIdMismatchTitle() {
        return getMessage("error.title.id-mismatch");
    }

    /** Gets the localized detail for ID mismatch errors. */
    public String getIdMismatchDetail(Object pathId, Object bodyId) {
        return getMessage("error.detail.id-mismatch", pathId, bodyId);
    }

    /** Gets the localized title for "precondition failed" errors. */
    public String getPreconditionFailedTitle() {
        return getMessage("error.title.precondition-failed");
    }

    /** Gets the localized title for "forbidden" errors. */
    public String getForbiddenTitle() {
        return getMessage("error.title.forbidden");
    }

    /** Gets the localized title for "internal error". */
    public String getInternalErrorTitle() {
        return getMessage("error.title.internal-error");
    }

    /** Gets the localized detail for internal errors. */
    public String getInternalErrorDetail() {
        return getMessage("error.detail.internal-error");
    }

    /** Gets the localized title for "bad request" errors. */
    public String getBadRequestTitle() {
        return getMessage("error.title.bad-request");
    }

    /** Gets the localized title for "malformed JSON" errors. */
    public String getMalformedJsonTitle() {
        return getMessage("error.title.malformed-json");
    }

    /** Gets the localized detail for malformed JSON errors. */
    public String getMalformedJsonDetail() {
        return getMessage("error.detail.malformed-json");
    }

    /** Gets the localized title for "type mismatch" errors. */
    public String getTypeMismatchTitle() {
        return getMessage("error.title.type-mismatch");
    }

    /** Gets the localized detail for type mismatch errors. */
    public String getTypeMismatchDetail(String paramName, Object value) {
        return getMessage("error.detail.type-mismatch", paramName, value);
    }

    /** Gets the localized title for "constraint violation" errors. */
    public String getConstraintViolationTitle() {
        return getMessage("error.title.constraint-violation");
    }

    /** Gets the localized detail for constraint violation errors. */
    public String getConstraintViolationDetail() {
        return getMessage("error.detail.constraint-violation");
    }

    /** Gets the localized title for "unauthorized" errors. */
    public String getUnauthorizedTitle() {
        return getMessage("error.title.unauthorized");
    }

    /** Gets the localized detail for unauthorized errors. */
    public String getUnauthorizedDetail() {
        return getMessage("error.detail.unauthorized");
    }

    /** Gets the localized title for "operation failed" errors. */
    public String getOperationFailedTitle() {
        return getMessage("error.title.operation-failed");
    }

    /** Gets the localized rate limit exceeded message. */
    public String getRateLimitExceeded() {
        return getMessage("ratelimit.exceeded");
    }

    /** Gets the localized rate limit retry after message. */
    public String getRateLimitRetryAfter(long seconds) {
        return getMessage("ratelimit.retry-after", seconds);
    }
}
