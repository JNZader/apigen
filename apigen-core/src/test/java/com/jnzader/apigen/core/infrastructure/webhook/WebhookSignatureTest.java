package com.jnzader.apigen.core.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("WebhookSignature Tests")
class WebhookSignatureTest {

    private static final String SECRET = "test-secret-key";
    private static final String PAYLOAD = "{\"event\":\"test\",\"data\":{\"id\":1}}";

    @Nested
    @DisplayName("Sign")
    class SignTests {

        @Test
        @DisplayName("should generate signature with timestamp")
        void shouldGenerateSignatureWithTimestamp() {
            Instant timestamp = Instant.ofEpochSecond(1700000000);

            String signature = WebhookSignature.sign(PAYLOAD, SECRET, timestamp);

            assertThat(signature).startsWith("t=1700000000,v1=").matches("t=\\d+,v1=[a-f0-9]{64}");
        }

        @Test
        @DisplayName("should generate deterministic signature for same input")
        void shouldGenerateDeterministicSignature() {
            Instant timestamp = Instant.ofEpochSecond(1700000000);

            String sig1 = WebhookSignature.sign(PAYLOAD, SECRET, timestamp);
            String sig2 = WebhookSignature.sign(PAYLOAD, SECRET, timestamp);

            assertThat(sig1).isEqualTo(sig2);
        }

        @Test
        @DisplayName("should generate different signatures for different secrets")
        void shouldGenerateDifferentSignaturesForDifferentSecrets() {
            Instant timestamp = Instant.ofEpochSecond(1700000000);

            String sig1 = WebhookSignature.sign(PAYLOAD, "secret1", timestamp);
            String sig2 = WebhookSignature.sign(PAYLOAD, "secret2", timestamp);

            assertThat(sig1).isNotEqualTo(sig2);
        }

        @Test
        @DisplayName("should generate different signatures for different payloads")
        void shouldGenerateDifferentSignaturesForDifferentPayloads() {
            Instant timestamp = Instant.ofEpochSecond(1700000000);

            String sig1 = WebhookSignature.sign("{\"a\":1}", SECRET, timestamp);
            String sig2 = WebhookSignature.sign("{\"a\":2}", SECRET, timestamp);

            assertThat(sig1).isNotEqualTo(sig2);
        }

        @Test
        @DisplayName("should generate signature with current timestamp when not provided")
        void shouldGenerateSignatureWithCurrentTimestamp() {
            long before = Instant.now().getEpochSecond();
            String signature = WebhookSignature.sign(PAYLOAD, SECRET);
            long after = Instant.now().getEpochSecond();

            Instant extractedTimestamp = WebhookSignature.extractTimestamp(signature);
            assertThat(extractedTimestamp).isNotNull();
            // Note: Different accessor - cannot chain
            assertThat(extractedTimestamp.getEpochSecond()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("Verify")
    class VerifyTests {

        @Test
        @DisplayName("should verify valid signature")
        void shouldVerifyValidSignature() {
            Instant timestamp = Instant.now();
            String signature = WebhookSignature.sign(PAYLOAD, SECRET, timestamp);

            boolean valid = WebhookSignature.verify(PAYLOAD, SECRET, signature);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should reject signature with wrong secret")
        void shouldRejectSignatureWithWrongSecret() {
            Instant timestamp = Instant.now();
            String signature = WebhookSignature.sign(PAYLOAD, SECRET, timestamp);

            boolean valid = WebhookSignature.verify(PAYLOAD, "wrong-secret", signature);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject signature with modified payload")
        void shouldRejectSignatureWithModifiedPayload() {
            Instant timestamp = Instant.now();
            String signature = WebhookSignature.sign(PAYLOAD, SECRET, timestamp);

            boolean valid = WebhookSignature.verify("{\"modified\":true}", SECRET, signature);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject expired signature")
        void shouldRejectExpiredSignature() {
            // Signature from 10 minutes ago
            Instant oldTimestamp = Instant.now().minusSeconds(600);
            String signature = WebhookSignature.sign(PAYLOAD, SECRET, oldTimestamp);

            // With 5 minute tolerance (default)
            boolean valid = WebhookSignature.verify(PAYLOAD, SECRET, signature);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should accept signature within tolerance")
        void shouldAcceptSignatureWithinTolerance() {
            // Signature from 2 minutes ago
            Instant recentTimestamp = Instant.now().minusSeconds(120);
            String signature = WebhookSignature.sign(PAYLOAD, SECRET, recentTimestamp);

            // With 5 minute tolerance (default)
            boolean valid = WebhookSignature.verify(PAYLOAD, SECRET, signature);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should accept signature with custom tolerance")
        void shouldAcceptSignatureWithCustomTolerance() {
            // Signature from 10 minutes ago
            Instant oldTimestamp = Instant.now().minusSeconds(600);
            String signature = WebhookSignature.sign(PAYLOAD, SECRET, oldTimestamp);

            // With 15 minute tolerance
            boolean valid = WebhookSignature.verify(PAYLOAD, SECRET, signature, 900);

            assertThat(valid).isTrue();
        }

        static Stream<Arguments> invalidSignatureHeaders() {
            return Stream.of(
                    Arguments.of(null, "null signature header"),
                    Arguments.of("", "empty signature header"),
                    Arguments.of("invalid-format", "malformed signature header"),
                    Arguments.of("v1=abc123", "signature header missing timestamp"),
                    Arguments.of("t=1700000000", "signature header missing signature"));
        }

        @ParameterizedTest(name = "should reject {1}")
        @MethodSource("invalidSignatureHeaders")
        @DisplayName("should reject invalid signature headers")
        void shouldRejectInvalidSignatureHeader(String signatureHeader, String description) {
            boolean valid = WebhookSignature.verify(PAYLOAD, SECRET, signatureHeader);

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("Extract Timestamp")
    class ExtractTimestampTests {

        @Test
        @DisplayName("should extract timestamp from valid header")
        void shouldExtractTimestampFromValidHeader() {
            Instant timestamp = Instant.ofEpochSecond(1700000000);
            String signature = WebhookSignature.sign(PAYLOAD, SECRET, timestamp);

            Instant extracted = WebhookSignature.extractTimestamp(signature);

            assertThat(extracted).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("should return null for invalid header")
        void shouldReturnNullForInvalidHeader() {
            Instant extracted = WebhookSignature.extractTimestamp("invalid");

            assertThat(extracted).isNull();
        }

        @Test
        @DisplayName("should return null for null header")
        void shouldReturnNullForNullHeader() {
            Instant extracted = WebhookSignature.extractTimestamp(null);

            assertThat(extracted).isNull();
        }
    }
}
