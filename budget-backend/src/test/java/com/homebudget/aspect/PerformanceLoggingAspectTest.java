package com.homebudget.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceLoggingAspectTest {

    private PerformanceLoggingAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private Object target;

    @BeforeEach
    void setUp() {
        aspect = new PerformanceLoggingAspect();
    }

    @Test
    @DisplayName("should call joinPoint.proceed() and return its result")
    void callsProceedAndReturnsResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        Object result = aspect.logMethodPerformance(joinPoint);

        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("should rethrow exception from joinPoint.proceed()")
    void rethrowsException() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("failingMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        assertThatThrownBy(() -> aspect.logMethodPerformance(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("test error");
    }

    @Test
    @DisplayName("should handle method with arguments")
    void handlesMethodWithArguments() throws Throwable {
        when(joinPoint.proceed()).thenReturn(42);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("methodWithArgs");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", 123L, null});

        Object result = aspect.logMethodPerformance(joinPoint);

        assertThat(result).isEqualTo(42);
    }

    @Test
    @DisplayName("should handle null return value")
    void handlesNullReturn() throws Throwable {
        when(joinPoint.proceed()).thenReturn(null);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("voidMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        Object result = aspect.logMethodPerformance(joinPoint);

        assertThat(result).isNull();
    }
}
