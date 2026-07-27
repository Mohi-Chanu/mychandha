package com.mychandha.platform.security.ratelimit;

import io.github.bucket4j.TimeMeter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"api", "local", "test"})
final class RateLimitService {

    private final RateLimitProperties properties;
    private final RateLimitKeyHasher hasher;
    private final RateLimitBucketRegistry buckets;
    private final RateLimitMetrics metrics;

    @Autowired
    RateLimitService(
            RateLimitProperties properties,
            RateLimitKeyHasher hasher,
            RateLimitMetrics metrics) {
        this(properties, hasher, metrics, TimeMeter.SYSTEM_NANOTIME);
    }

    RateLimitService(
            RateLimitProperties properties,
            RateLimitKeyHasher hasher,
            RateLimitMetrics metrics,
            TimeMeter timeMeter) {
        this.properties = properties;
        this.hasher = hasher;
        this.metrics = metrics;
        this.buckets = new RateLimitBucketRegistry(properties, timeMeter);
    }

    RateLimitDecision check(
            RateLimitScope scope,
            String rawKey,
            RateLimitEndpointClass endpointClass) {
        if (!properties.enabled()) {
            return RateLimitDecision.allowed(scope, endpointClass);
        }
        RateLimitDecision decision =
                buckets.consume(scope, hasher.digest(rawKey), endpointClass);
        metrics.record(decision);
        return decision;
    }

    boolean capacityExceeded() {
        return buckets.capacityExceeded();
    }

    long estimatedSize() {
        return buckets.estimatedSize();
    }

    int maximumSize() {
        return buckets.maximumSize();
    }

    boolean enabled() {
        return properties.enabled();
    }

    void cleanUp() {
        buckets.cleanUp();
    }
}
