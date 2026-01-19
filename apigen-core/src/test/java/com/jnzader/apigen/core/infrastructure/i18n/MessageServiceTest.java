package com.jnzader.apigen.core.infrastructure.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@DisplayName("MessageService Tests")
class MessageServiceTest {

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        MessageSource messageSource = createMessageSource();
        messageService = new MessageService(messageSource);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    private MessageSource createMessageSource() {
        ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.ENGLISH);
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    @Nested
    @DisplayName("English locale messages")
    class EnglishLocaleTests {

        @BeforeEach
        void setUp() {
            LocaleContextHolder.setLocale(Locale.ENGLISH);
        }

        @Test
        @DisplayName("Should return English not found title")
        void shouldReturnEnglishNotFoundTitle() {
            String title = messageService.getNotFoundTitle();
            assertThat(title).isEqualTo("Resource not found");
        }

        @Test
        @DisplayName("Should return English conflict title")
        void shouldReturnEnglishConflictTitle() {
            String title = messageService.getConflictTitle();
            assertThat(title).isEqualTo("Resource conflict");
        }

        @Test
        @DisplayName("Should return English validation title")
        void shouldReturnEnglishValidationTitle() {
            String title = messageService.getValidationTitle();
            assertThat(title).isEqualTo("Validation error");
        }

        @Test
        @DisplayName("Should return English internal error detail")
        void shouldReturnEnglishInternalErrorDetail() {
            String detail = messageService.getInternalErrorDetail();
            assertThat(detail).isEqualTo("An unexpected error occurred. Please try again later.");
        }

        @Test
        @DisplayName("Should return English validation detail with count")
        void shouldReturnEnglishValidationDetailWithCount() {
            String detail = messageService.getValidationDetail(3);
            assertThat(detail).isEqualTo("The request contains 3 validation error(s)");
        }

        @Test
        @DisplayName("Should return English ID mismatch detail")
        void shouldReturnEnglishIdMismatchDetail() {
            String detail = messageService.getIdMismatchDetail(1, 2);
            assertThat(detail).isEqualTo("Path ID (1) does not match body ID (2)");
        }

        @Test
        @DisplayName("Should return English type mismatch detail")
        void shouldReturnEnglishTypeMismatchDetail() {
            String detail = messageService.getTypeMismatchDetail("id", "abc");
            assertThat(detail).isEqualTo("Parameter 'id' has an invalid value: 'abc'");
        }
    }

    @Nested
    @DisplayName("Spanish locale messages")
    class SpanishLocaleTests {

        @BeforeEach
        void setUp() {
            LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
        }

        @Test
        @DisplayName("Should return Spanish not found title")
        void shouldReturnSpanishNotFoundTitle() {
            String title = messageService.getNotFoundTitle();
            assertThat(title).isEqualTo("Recurso no encontrado");
        }

        @Test
        @DisplayName("Should return Spanish conflict title")
        void shouldReturnSpanishConflictTitle() {
            String title = messageService.getConflictTitle();
            assertThat(title).isEqualTo("Conflicto de recurso");
        }

        @Test
        @DisplayName("Should return Spanish validation title")
        void shouldReturnSpanishValidationTitle() {
            String title = messageService.getValidationTitle();
            assertThat(title).isEqualTo("Error de validación");
        }

        @Test
        @DisplayName("Should return Spanish internal error detail")
        void shouldReturnSpanishInternalErrorDetail() {
            String detail = messageService.getInternalErrorDetail();
            assertThat(detail)
                    .isEqualTo(
                            "Ha ocurrido un error inesperado. Por favor, inténtelo de nuevo más"
                                    + " tarde.");
        }

        @Test
        @DisplayName("Should return Spanish validation detail with count")
        void shouldReturnSpanishValidationDetailWithCount() {
            String detail = messageService.getValidationDetail(3);
            assertThat(detail).isEqualTo("La solicitud contiene 3 error(es) de validación");
        }

        @Test
        @DisplayName("Should return Spanish ID mismatch detail")
        void shouldReturnSpanishIdMismatchDetail() {
            String detail = messageService.getIdMismatchDetail(1, 2);
            assertThat(detail).isEqualTo("El ID del path (1) no coincide con el ID del body (2)");
        }
    }

    @Nested
    @DisplayName("Message retrieval methods")
    class MessageRetrievalTests {

        @Test
        @DisplayName("Should get message by code")
        void shouldGetMessageByCode() {
            String message = messageService.getMessage("error.title.not-found");
            assertThat(message).isEqualTo("Resource not found");
        }

        @Test
        @DisplayName("Should get message with arguments")
        void shouldGetMessageWithArguments() {
            String message = messageService.getMessage("error.detail.not-found.with-id", "123");
            assertThat(message).isEqualTo("Resource with ID '123' not found");
        }

        @Test
        @DisplayName("Should get message for specific locale")
        void shouldGetMessageForSpecificLocale() {
            String message =
                    messageService.getMessage("error.title.not-found", Locale.forLanguageTag("es"));
            assertThat(message).isEqualTo("Recurso no encontrado");
        }

        @Test
        @DisplayName("Should return code when message not found")
        void shouldReturnCodeWhenMessageNotFound() {
            String message = messageService.getMessage("non.existent.code");
            assertThat(message).isEqualTo("non.existent.code");
        }

        @Test
        @DisplayName("Should get message or default")
        void shouldGetMessageOrDefault() {
            String message =
                    messageService.getMessageOrDefault("non.existent.code", "Default message");
            assertThat(message).isEqualTo("Default message");
        }

        @Test
        @DisplayName("Should get current locale")
        void shouldGetCurrentLocale() {
            LocaleContextHolder.setLocale(Locale.FRENCH);
            Locale locale = messageService.getCurrentLocale();
            assertThat(locale).isEqualTo(Locale.FRENCH);
        }
    }

    @Nested
    @DisplayName("Convenience methods")
    class ConvenienceMethodsTests {

        @Test
        @DisplayName("Should return forbidden title")
        void shouldReturnForbiddenTitle() {
            String title = messageService.getForbiddenTitle();
            assertThat(title).isEqualTo("Forbidden action");
        }

        @Test
        @DisplayName("Should return bad request title")
        void shouldReturnBadRequestTitle() {
            String title = messageService.getBadRequestTitle();
            assertThat(title).isEqualTo("Invalid argument");
        }

        @Test
        @DisplayName("Should return malformed JSON title")
        void shouldReturnMalformedJsonTitle() {
            String title = messageService.getMalformedJsonTitle();
            assertThat(title).isEqualTo("Malformed JSON");
        }

        @Test
        @DisplayName("Should return malformed JSON detail")
        void shouldReturnMalformedJsonDetail() {
            String detail = messageService.getMalformedJsonDetail();
            assertThat(detail).isEqualTo("The request body contains invalid or malformed JSON");
        }

        @Test
        @DisplayName("Should return constraint violation title")
        void shouldReturnConstraintViolationTitle() {
            String title = messageService.getConstraintViolationTitle();
            assertThat(title).isEqualTo("Invalid parameters");
        }

        @Test
        @DisplayName("Should return rate limit exceeded message")
        void shouldReturnRateLimitExceededMessage() {
            String message = messageService.getRateLimitExceeded();
            assertThat(message).isEqualTo("Too many requests. Please try again later.");
        }

        @Test
        @DisplayName("Should return rate limit retry after message")
        void shouldReturnRateLimitRetryAfterMessage() {
            String message = messageService.getRateLimitRetryAfter(30);
            assertThat(message).isEqualTo("Please retry after 30 seconds");
        }

        @Test
        @DisplayName("Should return precondition failed title")
        void shouldReturnPreconditionFailedTitle() {
            String title = messageService.getPreconditionFailedTitle();
            assertThat(title).isEqualTo("Precondition failed");
        }

        @Test
        @DisplayName("Should return operation failed title")
        void shouldReturnOperationFailedTitle() {
            String title = messageService.getOperationFailedTitle();
            assertThat(title).isEqualTo("Operation failed");
        }

        @Test
        @DisplayName("Should return unauthorized title")
        void shouldReturnUnauthorizedTitle() {
            String title = messageService.getUnauthorizedTitle();
            assertThat(title).isEqualTo("Unauthorized");
        }

        @Test
        @DisplayName("Should return unauthorized detail")
        void shouldReturnUnauthorizedDetail() {
            String detail = messageService.getUnauthorizedDetail();
            assertThat(detail).isEqualTo("Invalid or expired authentication token");
        }
    }
}
