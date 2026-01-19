package com.jnzader.apigen.codegen.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SqlIndex Tests")
class SqlIndexTest {

    @Nested
    @DisplayName("toJpaAnnotation()")
    class ToJpaAnnotationTests {

        @Test
        @DisplayName("Should generate basic index annotation")
        void shouldGenerateBasicIndexAnnotation() {
            SqlIndex index =
                    SqlIndex.builder()
                            .name("idx_product_name")
                            .columns(List.of("name"))
                            .unique(false)
                            .build();

            String result = index.toJpaAnnotation();

            assertThat(result)
                    .isEqualTo("@Index(name = \"idx_product_name\", columnList = \"name\")");
        }

        @Test
        @DisplayName("Should generate unique index annotation")
        void shouldGenerateUniqueIndexAnnotation() {
            SqlIndex index =
                    SqlIndex.builder()
                            .name("idx_user_email")
                            .columns(List.of("email"))
                            .unique(true)
                            .build();

            String result = index.toJpaAnnotation();

            assertThat(result)
                    .contains("unique = true")
                    .isEqualTo(
                            "@Index(name = \"idx_user_email\", columnList = \"email\", unique ="
                                    + " true)");
        }

        @Test
        @DisplayName("Should generate composite index annotation")
        void shouldGenerateCompositeIndexAnnotation() {
            SqlIndex index =
                    SqlIndex.builder()
                            .name("idx_order_user_date")
                            .columns(List.of("user_id", "order_date"))
                            .unique(false)
                            .build();

            String result = index.toJpaAnnotation();

            assertThat(result).contains("columnList = \"user_id, order_date\"");
        }
    }

    @Nested
    @DisplayName("IndexType enum")
    class IndexTypeTests {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            assertThat(SqlIndex.IndexType.values())
                    .containsExactlyInAnyOrder(
                            SqlIndex.IndexType.BTREE,
                            SqlIndex.IndexType.HASH,
                            SqlIndex.IndexType.GIN,
                            SqlIndex.IndexType.GIST,
                            SqlIndex.IndexType.BRIN);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Should build index with all properties")
        void shouldBuildIndexWithAllProperties() {
            SqlIndex index =
                    SqlIndex.builder()
                            .name("idx_active_users")
                            .tableName("users")
                            .columns(List.of("status", "last_login"))
                            .unique(false)
                            .type(SqlIndex.IndexType.BTREE)
                            .condition("status = 'active'")
                            .build();

            assertThat(index.getName()).isEqualTo("idx_active_users");
            assertThat(index.getTableName()).isEqualTo("users");
            assertThat(index.getColumns()).containsExactly("status", "last_login");
            assertThat(index.isUnique()).isFalse();
            assertThat(index.getType()).isEqualTo(SqlIndex.IndexType.BTREE);
            assertThat(index.getCondition()).isEqualTo("status = 'active'");
        }
    }
}
