package com.orv.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PerformanceLoggingAspect {

    @Around("@annotation(measurePerformance)")
    public Object measure(ProceedingJoinPoint joinPoint, MeasurePerformance measurePerformance) throws Throwable {
        String operation = measurePerformance.value().isEmpty()
                ? joinPoint.getSignature().getName()
                : measurePerformance.value();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        long startNs = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("perf operation={} class={} duration_ms={}", operation, className, durationMs);
        }
    }
}
