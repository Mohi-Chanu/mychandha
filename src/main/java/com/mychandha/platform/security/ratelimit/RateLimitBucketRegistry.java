package com.mychandha.platform.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class RateLimitBucketRegistry {

    private final Cache<String, Bucket> buckets;
    private final Map<RateLimitScope, RateLimitProperties.Limit> limits;
    private final int maximumSize;
    private final TimeMeter timeMeter;
    private final AtomicBoolean capacityExceeded = new AtomicBoolean();

    RateLimitBucketRegistry(RateLimitProperties properties, TimeMeter timeMeter) {
        maximumSize = properties.cacheMaximumSize();
        this.timeMeter = timeMeter;
        buckets = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterAccess(properties.cacheExpireAfterAccess())
                .ticker(timeMeter::currentTimeNanos)
                .build();
        limits = limits(properties);
    }

    RateLimitDecision consume(
            RateLimitScope scope,
            String key,
            RateLimitEndpointClass endpointClass) {
        String bucketKey = bucketKey(scope, key, endpointClass);
        if (buckets.getIfPresent(bucketKey) == null
                && buckets.estimatedSize() >= maximumSize) {
            capacityExceeded.set(true);
            return RateLimitDecision.rejected(
                    60, scope, endpointClass, "capacity_rejected");
        }

        RateLimitProperties.Limit limit = limits.get(scope);
        Bucket bucket = buckets.get(bucketKey, ignored -> Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(builder -> builder.capacity(limit.capacity())
                        .refillGreedy(limit.capacity(), limit.refillPeriod()))
                .build());
        ConsumptionProbe probe = bucket
                .tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return RateLimitDecision.allowed(scope, endpointClass);
        }
        long retrySeconds = Math.max(
                1,
                TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill() - 1) + 1);
        return RateLimitDecision.rejected(
                retrySeconds, scope, endpointClass, "limited");
    }

    boolean capacityExceeded() {
        return capacityExceeded.get();
    }

    long estimatedSize() {
        return buckets.estimatedSize();
    }

    int maximumSize() {
        return maximumSize;
    }

    void cleanUp() {
        buckets.cleanUp();
    }

    private static Map<RateLimitScope, RateLimitProperties.Limit> limits(
            RateLimitProperties properties) {
        Map<RateLimitScope, RateLimitProperties.Limit> result =
                new EnumMap<>(RateLimitScope.class);
        for (RateLimitScope scope : RateLimitScope.values()) {
            result.put(scope, properties.limitFor(scope));
        }
        return Map.copyOf(result);
    }

    private static String bucketKey(
            RateLimitScope scope,
            String key,
            RateLimitEndpointClass endpointClass) {
        if (scope == RateLimitScope.PROCESS) {
            return scope.tag() + ':' + key;
        }
        return scope.tag() + ':' + endpointClass.tag() + ':' + key;
    }
}
