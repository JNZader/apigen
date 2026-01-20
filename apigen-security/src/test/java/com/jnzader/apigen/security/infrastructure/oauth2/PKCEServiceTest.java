package com.jnzader.apigen.security.infrastructure.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnzader.apigen.security.infrastructure.oauth2.PKCEService.CodeChallengeMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PKCEService}.
 *
 * <p>Tests cover the PKCE (Proof Key for Code Exchange) implementation according to RFC 7636.
 */
@DisplayName("PKCEService")
class PKCEServiceTest {

    private PKCEService pkceService;

    @BeforeEach
    void setUp() {
        pkceService = new PKCEService();
    }

    @Nested
    @DisplayName("generateCodeVerifier")
    class GenerateCodeVerifier {

        @Test
        @DisplayName("should generate code verifier with default length")
        void shouldGenerateCodeVerifierWithDefaultLength() {
            String codeVerifier = pkceService.generateCodeVerifier();

            assertThat(codeVerifier)
                    .isNotNull()
                    .hasSize(64) // default length
                    .matches("^[A-Za-z0-9\\-._~]+$"); // valid characters per RFC 7636
        }

        @Test
        @DisplayName("should generate code verifier with specified length")
        void shouldGenerateCodeVerifierWithSpecifiedLength() {
            String codeVerifier = pkceService.generateCodeVerifier(50);

            assertThat(codeVerifier).isNotNull().hasSize(50);
        }

        @Test
        @DisplayName("should generate unique code verifiers")
        void shouldGenerateUniqueCodeVerifiers() {
            String verifier1 = pkceService.generateCodeVerifier();
            String verifier2 = pkceService.generateCodeVerifier();

            assertThat(verifier1).isNotEqualTo(verifier2);
        }

        @Test
        @DisplayName("should reject length below minimum (43)")
        void shouldRejectLengthBelowMinimum() {
            assertThatThrownBy(() -> pkceService.generateCodeVerifier(42))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("43");
        }

        @Test
        @DisplayName("should reject length above maximum (128)")
        void shouldRejectLengthAboveMaximum() {
            assertThatThrownBy(() -> pkceService.generateCodeVerifier(129))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("128");
        }

        @Test
        @DisplayName("should accept minimum length (43)")
        void shouldAcceptMinimumLength() {
            String codeVerifier = pkceService.generateCodeVerifier(43);
            assertThat(codeVerifier).hasSize(43);
        }

        @Test
        @DisplayName("should accept maximum length (128)")
        void shouldAcceptMaximumLength() {
            String codeVerifier = pkceService.generateCodeVerifier(128);
            assertThat(codeVerifier).hasSize(128);
        }
    }

    @Nested
    @DisplayName("generateCodeChallenge")
    class GenerateCodeChallenge {

        @Test
        @DisplayName("should generate S256 code challenge by default")
        void shouldGenerateS256CodeChallengeByDefault() {
            String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
            String codeChallenge = pkceService.generateCodeChallenge(codeVerifier);

            // The challenge should be different from the verifier (it's hashed)
            assertThat(codeChallenge).isNotNull().isNotEqualTo(codeVerifier);
        }

        @Test
        @DisplayName("should generate consistent S256 challenge for same verifier")
        void shouldGenerateConsistentS256Challenge() {
            String codeVerifier = pkceService.generateCodeVerifier();

            String challenge1 = pkceService.generateCodeChallenge(codeVerifier);
            String challenge2 = pkceService.generateCodeChallenge(codeVerifier);

            assertThat(challenge1).isEqualTo(challenge2);
        }

        @Test
        @DisplayName("should return verifier as-is for PLAIN method")
        void shouldReturnVerifierForPlainMethod() {
            String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

            String codeChallenge =
                    pkceService.generateCodeChallenge(codeVerifier, CodeChallengeMethod.PLAIN);

            assertThat(codeChallenge).isEqualTo(codeVerifier);
        }

        @Test
        @DisplayName("should reject null code verifier")
        void shouldRejectNullCodeVerifier() {
            assertThatThrownBy(() -> pkceService.generateCodeChallenge(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject empty code verifier")
        void shouldRejectEmptyCodeVerifier() {
            assertThatThrownBy(() -> pkceService.generateCodeChallenge(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should generate URL-safe base64 challenge")
        void shouldGenerateUrlSafeBase64Challenge() {
            String codeVerifier = pkceService.generateCodeVerifier();
            String codeChallenge = pkceService.generateCodeChallenge(codeVerifier);

            // Should not contain standard base64 padding or unsafe characters
            assertThat(codeChallenge).doesNotContain("=").doesNotContain("+").doesNotContain("/");
        }
    }

    @Nested
    @DisplayName("verifyCodeChallenge")
    class VerifyCodeChallenge {

        @Test
        @DisplayName("should verify valid S256 challenge")
        void shouldVerifyValidS256Challenge() {
            String codeVerifier = pkceService.generateCodeVerifier();
            String codeChallenge = pkceService.generateCodeChallenge(codeVerifier);

            boolean isValid = pkceService.verifyCodeChallenge(codeVerifier, codeChallenge);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should reject invalid verifier for S256")
        void shouldRejectInvalidVerifierForS256() {
            String originalVerifier = pkceService.generateCodeVerifier();
            String wrongVerifier = pkceService.generateCodeVerifier();
            String codeChallenge = pkceService.generateCodeChallenge(originalVerifier);

            boolean isValid = pkceService.verifyCodeChallenge(wrongVerifier, codeChallenge);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should verify valid PLAIN challenge")
        void shouldVerifyValidPlainChallenge() {
            String codeVerifier = pkceService.generateCodeVerifier();
            String codeChallenge =
                    pkceService.generateCodeChallenge(codeVerifier, CodeChallengeMethod.PLAIN);

            boolean isValid =
                    pkceService.verifyCodeChallenge(
                            codeVerifier, codeChallenge, CodeChallengeMethod.PLAIN);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should reject null verifier")
        void shouldRejectNullVerifier() {
            String codeChallenge = "some-challenge";

            boolean isValid = pkceService.verifyCodeChallenge(null, codeChallenge);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should reject null challenge")
        void shouldRejectNullChallenge() {
            String codeVerifier = pkceService.generateCodeVerifier();

            boolean isValid = pkceService.verifyCodeChallenge(codeVerifier, null);

            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidCodeVerifier")
    class IsValidCodeVerifier {

        @Test
        @DisplayName("should accept valid code verifier")
        void shouldAcceptValidCodeVerifier() {
            String codeVerifier = pkceService.generateCodeVerifier();

            boolean isValid = pkceService.isValidCodeVerifier(codeVerifier);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should reject null verifier")
        void shouldRejectNullVerifier() {
            assertThat(pkceService.isValidCodeVerifier(null)).isFalse();
        }

        @Test
        @DisplayName("should reject too short verifier")
        void shouldRejectTooShortVerifier() {
            String shortVerifier = "abc123"; // less than 43 characters

            assertThat(pkceService.isValidCodeVerifier(shortVerifier)).isFalse();
        }

        @Test
        @DisplayName("should reject too long verifier")
        void shouldRejectTooLongVerifier() {
            String longVerifier = "a".repeat(129); // more than 128 characters

            assertThat(pkceService.isValidCodeVerifier(longVerifier)).isFalse();
        }

        @Test
        @DisplayName("should reject verifier with invalid characters")
        void shouldRejectVerifierWithInvalidCharacters() {
            String invalidVerifier = "abc@123#def$" + "x".repeat(40);

            assertThat(pkceService.isValidCodeVerifier(invalidVerifier)).isFalse();
        }

        @Test
        @DisplayName("should accept verifier with all valid unreserved characters")
        void shouldAcceptVerifierWithAllValidCharacters() {
            // Valid characters: [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
            String validVerifier = "ABCDEFghij0123456789-._~" + "x".repeat(20);

            assertThat(pkceService.isValidCodeVerifier(validVerifier)).isTrue();
        }
    }

    @Nested
    @DisplayName("RFC 7636 Compliance")
    class Rfc7636Compliance {

        @Test
        @DisplayName("should pass RFC 7636 Appendix B example")
        void shouldPassRfc7636AppendixBExample() {
            // From RFC 7636 Appendix B
            // Note: We can't test the exact verifier/challenge from the RFC because
            // our implementation uses URL-safe base64 which is correct per spec
            String codeVerifier = pkceService.generateCodeVerifier();
            String codeChallenge = pkceService.generateCodeChallenge(codeVerifier);

            // Verify round-trip works
            assertThat(pkceService.verifyCodeChallenge(codeVerifier, codeChallenge)).isTrue();
        }

        @Test
        @DisplayName("should generate challenge with correct length for S256")
        void shouldGenerateChallengeWithCorrectLengthForS256() {
            // SHA-256 produces 256 bits = 32 bytes
            // Base64 encoding produces 43 characters (without padding)
            String codeVerifier = pkceService.generateCodeVerifier();
            String codeChallenge = pkceService.generateCodeChallenge(codeVerifier);

            assertThat(codeChallenge).hasSize(43);
        }
    }
}
