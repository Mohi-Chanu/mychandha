package com.mychandha.platform.security.ratelimit;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"api", "local", "test"})
final class RateLimitMetrics {

    private final MeterRegistry registry;

    RateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    void record(RateLimitDecision decision) {
        registry.counter(
                        "mychandha.rate.limit.requests",
                        "scope", decision.scope().tag(),
                        "endpoint_class", decision.endpointClass().tag(),
                        "outcome", decision.outcome())
                .increment();
    }
}
