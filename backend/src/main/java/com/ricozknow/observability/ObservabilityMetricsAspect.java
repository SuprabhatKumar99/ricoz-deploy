package com.ricozknow.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;


/** Records low-cardinality service execution metrics for the application layer. */
@Aspect
@Component
public class ObservabilityMetricsAspect {

    private final MeterRegistry registry;

    public ObservabilityMetricsAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("execution(public * com.ricozknow..*(..)) && @within(org.springframework.stereotype.Service)")
    public Object observeServiceCall(ProceedingJoinPoint pjp) throws Throwable {
        String service = pjp.getTarget().getClass().getSimpleName();
        String operation = pjp.getSignature().getName();
        Timer.Sample sample = Timer.start(registry);
        String outcome = "success";
        try {
            return pjp.proceed();
        } catch (Throwable ex) {
            outcome = "error";
            registry.counter("ricozknow_service_errors_total", "service", service, "operation", operation,
                    "exception", ex.getClass().getSimpleName()).increment();
            throw ex;
        } finally {
            sample.stop(Timer.builder("ricozknow_service_calls")
                    .description("Application service call latency")
                    .tag("service", service)
                    .tag("operation", operation)
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .publishPercentileHistogram()
                    .register(registry));
        }
    }
}
