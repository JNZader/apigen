package com.jnzader.apigen.codegen.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RelationType Tests")
class RelationTypeTest {

    @Nested
    @DisplayName("getAnnotation()")
    class GetAnnotationTests {

        @ParameterizedTest
        @CsvSource({
                "ONE_TO_ONE, @OneToOne",
                "ONE_TO_MANY, @OneToMany",
                "MANY_TO_ONE, @ManyToOne",
                "MANY_TO_MANY, @ManyToMany"
        })
        @DisplayName("Should return correct JPA annotation")
        void shouldReturnCorrectJpaAnnotation(RelationType type, String expectedAnnotation) {
            assertThat(type.getAnnotation()).isEqualTo(expectedAnnotation);
        }
    }

    @Nested
    @DisplayName("inverse()")
    class InverseTests {

        @Test
        @DisplayName("Should return ONE_TO_ONE for ONE_TO_ONE")
        void shouldReturnOneToOneForOneToOne() {
            assertThat(RelationType.ONE_TO_ONE.inverse()).isEqualTo(RelationType.ONE_TO_ONE);
        }

        @Test
        @DisplayName("Should return MANY_TO_ONE for ONE_TO_MANY")
        void shouldReturnManyToOneForOneToMany() {
            assertThat(RelationType.ONE_TO_MANY.inverse()).isEqualTo(RelationType.MANY_TO_ONE);
        }

        @Test
        @DisplayName("Should return ONE_TO_MANY for MANY_TO_ONE")
        void shouldReturnOneToManyForManyToOne() {
            assertThat(RelationType.MANY_TO_ONE.inverse()).isEqualTo(RelationType.ONE_TO_MANY);
        }

        @Test
        @DisplayName("Should return MANY_TO_MANY for MANY_TO_MANY")
        void shouldReturnManyToManyForManyToMany() {
            assertThat(RelationType.MANY_TO_MANY.inverse()).isEqualTo(RelationType.MANY_TO_MANY);
        }
    }

    @Nested
    @DisplayName("isCollection()")
    class IsCollectionTests {

        @Test
        @DisplayName("Should return true for ONE_TO_MANY")
        void shouldReturnTrueForOneToMany() {
            assertThat(RelationType.ONE_TO_MANY.isCollection()).isTrue();
        }

        @Test
        @DisplayName("Should return true for MANY_TO_MANY")
        void shouldReturnTrueForManyToMany() {
            assertThat(RelationType.MANY_TO_MANY.isCollection()).isTrue();
        }

        @Test
        @DisplayName("Should return false for ONE_TO_ONE")
        void shouldReturnFalseForOneToOne() {
            assertThat(RelationType.ONE_TO_ONE.isCollection()).isFalse();
        }

        @Test
        @DisplayName("Should return false for MANY_TO_ONE")
        void shouldReturnFalseForManyToOne() {
            assertThat(RelationType.MANY_TO_ONE.isCollection()).isFalse();
        }
    }

    @Nested
    @DisplayName("Enum values")
    class EnumValuesTests {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            assertThat(RelationType.values()).containsExactlyInAnyOrder(
                    RelationType.ONE_TO_ONE,
                    RelationType.ONE_TO_MANY,
                    RelationType.MANY_TO_ONE,
                    RelationType.MANY_TO_MANY
            );
        }
    }
}
