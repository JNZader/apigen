package com.jnzader.apigen.core.infrastructure.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TenantContext Tests")
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("Set and Get Tenant")
    class SetAndGetTests {

        @Test
        @DisplayName("should set and get tenant ID")
        void shouldSetAndGetTenantId() {
            TenantContext.setTenantId("acme-corp");

            assertThat(TenantContext.getTenantId()).isEqualTo("acme-corp");
        }

        @Test
        @DisplayName("should return null when not set")
        void shouldReturnNullWhenNotSet() {
            assertThat(TenantContext.getTenantId()).isNull();
        }

        @Test
        @DisplayName("should return Optional when using getCurrentTenant")
        void shouldReturnOptionalWhenUsingGetCurrentTenant() {
            assertThat(TenantContext.getCurrentTenant()).isEmpty();

            TenantContext.setTenantId("tenant1");

            assertThat(TenantContext.getCurrentTenant()).isEqualTo(Optional.of("tenant1"));
        }

        @Test
        @DisplayName("should throw on null tenant ID")
        void shouldThrowOnNullTenantId() {
            assertThatThrownBy(() -> TenantContext.setTenantId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("should throw on blank tenant ID")
        void shouldThrowOnBlankTenantId() {
            assertThatThrownBy(() -> TenantContext.setTenantId("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("should clear tenant ID")
        void shouldClearTenantId() {
            TenantContext.setTenantId("tenant1");
            TenantContext.clear();

            assertThat(TenantContext.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("Require Tenant")
    class RequireTenantTests {

        @Test
        @DisplayName("should return tenant when set")
        void shouldReturnTenantWhenSet() {
            TenantContext.setTenantId("required-tenant");

            assertThat(TenantContext.requireTenantId()).isEqualTo("required-tenant");
        }

        @Test
        @DisplayName("should throw when tenant not set")
        void shouldThrowWhenTenantNotSet() {
            assertThatThrownBy(TenantContext::requireTenantId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No tenant context available");
        }
    }

    @Nested
    @DisplayName("Default Tenant")
    class DefaultTenantTests {

        @Test
        @DisplayName("should return tenant when set")
        void shouldReturnTenantWhenSet() {
            TenantContext.setTenantId("my-tenant");

            assertThat(TenantContext.getTenantIdOrDefault("default")).isEqualTo("my-tenant");
        }

        @Test
        @DisplayName("should return default when not set")
        void shouldReturnDefaultWhenNotSet() {
            assertThat(TenantContext.getTenantIdOrDefault("default")).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("Tenant Checking")
    class TenantCheckingTests {

        @Test
        @DisplayName("should check if tenant is set")
        void shouldCheckIfTenantIsSet() {
            assertThat(TenantContext.hasTenant()).isFalse();

            TenantContext.setTenantId("tenant1");

            assertThat(TenantContext.hasTenant()).isTrue();
        }

        @Test
        @DisplayName("should check if current tenant matches")
        void shouldCheckIfCurrentTenantMatches() {
            TenantContext.setTenantId("tenant1");

            assertThat(TenantContext.isTenant("tenant1")).isTrue();
            assertThat(TenantContext.isTenant("tenant2")).isFalse();
        }

        @Test
        @DisplayName("should return false for isTenant when not set")
        void shouldReturnFalseForIsTenantWhenNotSet() {
            assertThat(TenantContext.isTenant("tenant1")).isFalse();
        }
    }

    @Nested
    @DisplayName("With Tenant")
    class WithTenantTests {

        @Test
        @DisplayName("should execute runnable with tenant context")
        void shouldExecuteRunnableWithTenantContext() {
            AtomicReference<String> capturedTenant = new AtomicReference<>();

            TenantContext.withTenant(
                    "temp-tenant", () -> capturedTenant.set(TenantContext.getTenantId()));

            assertThat(capturedTenant.get()).isEqualTo("temp-tenant");
            assertThat(TenantContext.getTenantId()).isNull();
        }

        @Test
        @DisplayName("should restore previous tenant after execution")
        void shouldRestorePreviousTenantAfterExecution() {
            TenantContext.setTenantId("original");

            TenantContext.withTenant(
                    "temp",
                    () -> {
                        assertThat(TenantContext.getTenantId()).isEqualTo("temp");
                    });

            assertThat(TenantContext.getTenantId()).isEqualTo("original");
        }

        @Test
        @DisplayName("should clear tenant even if exception occurs")
        void shouldClearTenantEvenIfExceptionOccurs() {
            try {
                TenantContext.withTenant(
                        "temp",
                        () -> {
                            throw new RuntimeException("Test exception");
                        });
            } catch (RuntimeException ignored) {
            }

            assertThat(TenantContext.getTenantId()).isNull();
        }

        @Test
        @DisplayName("should execute supplier with tenant context")
        void shouldExecuteSupplierWithTenantContext() {
            String result = TenantContext.withTenant("supplier-tenant", TenantContext::getTenantId);

            assertThat(result).isEqualTo("supplier-tenant");
            assertThat(TenantContext.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("Thread Isolation")
    class ThreadIsolationTests {

        @Test
        @DisplayName("should isolate tenant per thread")
        void shouldIsolateTenantPerThread() throws InterruptedException {
            TenantContext.setTenantId("main-tenant");

            Thread otherThread =
                    new Thread(
                            () -> {
                                // InheritableThreadLocal: child inherits parent's value
                                assertThat(TenantContext.getTenantId()).isEqualTo("main-tenant");

                                // But can override
                                TenantContext.setTenantId("other-tenant");
                                assertThat(TenantContext.getTenantId()).isEqualTo("other-tenant");
                            });

            otherThread.start();
            otherThread.join();

            // Main thread should still have main-tenant
            assertThat(TenantContext.getTenantId()).isEqualTo("main-tenant");
        }
    }
}
