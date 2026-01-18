package com.jnzader.apigen.core.infrastructure.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("MetricsAspect Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetricsAspectTest {

    private MeterRegistry meterRegistry;
    private MetricsAspect metricsAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsAspect = new MetricsAspect(meterRegistry);
        ReflectionTestUtils.setField(metricsAspect, "metricsEnabled", true);
        ReflectionTestUtils.setField(metricsAspect, "slowThresholdMs", 500L);
    }

    @Nested
    @DisplayName("measureAnnotatedMethod")
    class MeasureAnnotatedMethodTests {

        @Test
        @DisplayName("should proceed without metrics when disabled")
        void shouldProceedWithoutMetricsWhenDisabled() throws Throwable {
            ReflectionTestUtils.setField(metricsAspect, "metricsEnabled", false);
            when(joinPoint.proceed()).thenReturn("result");

            Measured measured = createMeasured("", false, 0);
            Object result = metricsAspect.measureAnnotatedMethod(joinPoint, measured);

            assertThat(result).isEqualTo("result");
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("should measure method execution time")
        void shouldMeasureMethodExecutionTime() throws Throwable {
            setupJoinPointMocks("testMethod");
            when(joinPoint.proceed()).thenReturn("result");

            Measured measured = createMeasured("custom-metric", false, 0);
            Object result = metricsAspect.measureAnnotatedMethod(joinPoint, measured);

            assertThat(result).isEqualTo("result");
            assertThat(meterRegistry.find("apigen.method.duration").timer()).isNotNull();
            assertThat(meterRegistry.find("apigen.method.calls").counter()).isNotNull();
        }

        @Test
        @DisplayName("should use custom metric name from annotation")
        void shouldUseCustomMetricNameFromAnnotation() throws Throwable {
            setupJoinPointMocks("testMethod");
            when(joinPoint.proceed()).thenReturn("result");

            Measured measured = createMeasured("my-custom-name", false, 0);
            metricsAspect.measureAnnotatedMethod(joinPoint, measured);

            assertThat(meterRegistry.find("apigen.method.duration")
                    .tag("name", "my-custom-name")
                    .timer()).isNotNull();
        }

        @Test
        @DisplayName("should record error metrics on exception")
        void shouldRecordErrorMetricsOnException() throws Throwable {
            setupJoinPointMocks("testMethod");
            when(joinPoint.proceed()).thenThrow(new RuntimeException("Test error"));

            Measured measured = createMeasured("error-test", false, 0);

            assertThatThrownBy(() -> metricsAspect.measureAnnotatedMethod(joinPoint, measured))
                    .isInstanceOf(RuntimeException.class);

            assertThat(meterRegistry.find("apigen.method.errors").counter()).isNotNull();
        }

        @Test
        @DisplayName("should use custom slow threshold from annotation")
        void shouldUseCustomSlowThresholdFromAnnotation() throws Throwable {
            setupJoinPointMocks("testMethod");
            when(joinPoint.proceed()).thenReturn("result");

            Measured measured = createMeasured("threshold-test", false, 100);
            metricsAspect.measureAnnotatedMethod(joinPoint, measured);

            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("should use class and method name when annotation name is blank")
        void shouldUseClassAndMethodNameWhenAnnotationNameIsBlank() throws Throwable {
            // Note: The mock uses Object.class.getMethod("toString"), so method name is "toString"
            setupJoinPointMocks("processData");
            when(joinPoint.proceed()).thenReturn("result");

            Measured measured = createMeasured("", false, 0);
            metricsAspect.measureAnnotatedMethod(joinPoint, measured);

            // The metric name is generated from class.getSimpleName() + "." + method.getName()
            // Since we mock with Object.class.getMethod("toString"), the method name is "toString"
            assertThat(meterRegistry.find("apigen.method.duration")
                    .tag("name", "TestTarget.toString")
                    .timer()).isNotNull();
        }

        @Test
        @DisplayName("should publish histogram when enabled")
        void shouldPublishHistogramWhenEnabled() throws Throwable {
            setupJoinPointMocks("histogramMethod");
            when(joinPoint.proceed()).thenReturn("result");

            Measured measured = createMeasured("histogram-test", true, 0);
            metricsAspect.measureAnnotatedMethod(joinPoint, measured);

            assertThat(meterRegistry.find("apigen.method.duration")
                    .tag("name", "histogram-test")
                    .timer()).isNotNull();
        }
    }

    @Nested
    @DisplayName("measureClassMethod")
    class MeasureClassMethodTests {

        @Test
        @DisplayName("should proceed without metrics when disabled")
        void shouldProceedWithoutMetricsWhenDisabled() throws Throwable {
            ReflectionTestUtils.setField(metricsAspect, "metricsEnabled", false);
            when(joinPoint.proceed()).thenReturn("result");

            Measured measured = createMeasured("", false, 0);
            Object result = metricsAspect.measureClassMethod(joinPoint, measured);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("should measure class method with generated name")
        void shouldMeasureClassMethodWithGeneratedName() throws Throwable {
            setupJoinPointMocks("someMethod");
            when(joinPoint.proceed()).thenReturn("result");

            Measured measured = createMeasured("", false, 0);
            Object result = metricsAspect.measureClassMethod(joinPoint, measured);

            assertThat(result).isEqualTo("result");
            assertThat(meterRegistry.find("apigen.method.duration")
                    .tag("name", "TestTarget.someMethod")
                    .tag("layer", "custom")
                    .timer()).isNotNull();
        }

        @Test
        @DisplayName("should use custom slow threshold from class annotation")
        void shouldUseCustomSlowThresholdFromClassAnnotation() throws Throwable {
            setupJoinPointMocks("slowMethod");
            when(joinPoint.proceed()).thenReturn("result");

            Measured measured = createMeasured("", false, 200);
            metricsAspect.measureClassMethod(joinPoint, measured);

            verify(joinPoint).proceed();
            assertThat(meterRegistry.find("apigen.method.calls")
                    .tag("outcome", "success")
                    .counter()).isNotNull();
        }

        @Test
        @DisplayName("should record error metrics on exception")
        void shouldRecordErrorMetricsOnException() throws Throwable {
            setupJoinPointMocks("failingMethod");
            when(joinPoint.proceed()).thenThrow(new IllegalStateException("Class method error"));

            Measured measured = createMeasured("", false, 0);

            assertThatThrownBy(() -> metricsAspect.measureClassMethod(joinPoint, measured))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(meterRegistry.find("apigen.method.errors")
                    .tag("exception", "IllegalStateException")
                    .counter()).isNotNull();
        }
    }

    @Nested
    @DisplayName("measureControllerEndpoint")
    class MeasureControllerEndpointTests {

        @Test
        @DisplayName("should proceed without metrics when disabled")
        void shouldProceedWithoutMetricsWhenDisabled() throws Throwable {
            ReflectionTestUtils.setField(metricsAspect, "metricsEnabled", false);
            when(joinPoint.proceed()).thenReturn("result");

            Object result = metricsAspect.measureControllerEndpoint(joinPoint);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("should measure controller endpoint")
        void shouldMeasureControllerEndpoint() throws Throwable {
            setupJoinPointMocks("findAll");
            when(joinPoint.proceed()).thenReturn("result");

            Object result = metricsAspect.measureControllerEndpoint(joinPoint);

            assertThat(result).isEqualTo("result");
            assertThat(meterRegistry.find("apigen.method.duration")
                    .tag("layer", "controller")
                    .timer()).isNotNull();
        }

        @Test
        @DisplayName("should record error metrics on controller exception")
        void shouldRecordErrorMetricsOnControllerException() throws Throwable {
            setupJoinPointMocks("create");
            when(joinPoint.proceed()).thenThrow(new RuntimeException("Controller error"));

            assertThatThrownBy(() -> metricsAspect.measureControllerEndpoint(joinPoint))
                    .isInstanceOf(RuntimeException.class);

            assertThat(meterRegistry.find("apigen.method.errors")
                    .tag("layer", "controller")
                    .counter()).isNotNull();
        }
    }

    @Nested
    @DisplayName("measureServiceWriteOperation")
    class MeasureServiceWriteOperationTests {

        @Test
        @DisplayName("should proceed without metrics when disabled")
        void shouldProceedWithoutMetricsWhenDisabled() throws Throwable {
            ReflectionTestUtils.setField(metricsAspect, "metricsEnabled", false);
            when(joinPoint.proceed()).thenReturn("result");

            Object result = metricsAspect.measureServiceWriteOperation(joinPoint);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("should measure service write operation")
        void shouldMeasureServiceWriteOperation() throws Throwable {
            setupJoinPointMocks("save");
            when(joinPoint.proceed()).thenReturn("result");

            Object result = metricsAspect.measureServiceWriteOperation(joinPoint);

            assertThat(result).isEqualTo("result");
            assertThat(meterRegistry.find("apigen.method.duration")
                    .tag("layer", "service")
                    .timer()).isNotNull();
        }

        @Test
        @DisplayName("should record error metrics on service exception")
        void shouldRecordErrorMetricsOnServiceException() throws Throwable {
            setupJoinPointMocks("updateOrder");
            when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("Service error"));

            assertThatThrownBy(() -> metricsAspect.measureServiceWriteOperation(joinPoint))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(meterRegistry.find("apigen.method.errors")
                    .tag("layer", "service")
                    .tag("exception", "IllegalArgumentException")
                    .counter()).isNotNull();
        }
    }

    private void setupJoinPointMocks(String methodName) throws NoSuchMethodException {
        // Use a real object as target - the class simple name will be used
        // Since we can't mock Class.class, we verify metrics are recorded with actual class name
        Object target = new TestTarget();
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn(methodName);

        Method method = Object.class.getMethod("toString");
        when(methodSignature.getMethod()).thenReturn(method);
    }

    /**
     * Helper class for testing metrics aspect.
     * The class simple name "TestTarget" is used in metric tags.
     */
    static class TestTarget {
        // Intentionally empty - used only to provide a class name for metric tags
    }

    private Measured createMeasured(String name, boolean histogram, long slowThresholdMs) {
        return new Measured() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Measured.class;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return "";
            }

            @Override
            public boolean histogram() {
                return histogram;
            }

            @Override
            public long slowThresholdMs() {
                return slowThresholdMs;
            }
        };
    }
}
