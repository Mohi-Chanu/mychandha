package com.mychandha.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    @Test
    void refillsDeterministicallyAndEmitsBoundedMetrics() {
        MutableTimeMeter time = new MutableTimeMeter();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            RateLimitService service = service(
                    RateLimitTestSupport.properties(2), meters, time);

            assertThat(check(service, "actor").allowed()).isTrue();
            assertThat(check(service, "actor").allowed()).isTrue();
            RateLimitDecision rejected = check(service, "actor");
            assertThat(rejected.allowed()).isFalse();
            assertThat(rejected.retryAfterSeconds()).isEqualTo(30);

            time.advance(Duration.ofSeconds(30));

            assertThat(check(service, "actor").allowed()).isTrue();
            assertThat(meters.find("mychandha.rate.limit.requests").counters())
                    .extracting(counter -> counter.getId().getTag("outcome"))
                    .containsExactlyInAnyOrder("allowed", "limited");
            assertThat(meters.getMeters()).allSatisfy(meter ->
                    assertThat(meter.getId().getTags())
                            .noneMatch(tag -> tag.getValue().equals("actor")));
        } finally {
            meters.close();
        }
    }

    @Test
    void concurrentConsumptionNeverExceedsCapacity() throws Exception {
        MutableTimeMeter time = new MutableTimeMeter();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try (var executor = Executors.newFixedThreadPool(20)) {
            RateLimitService service = service(
                    RateLimitTestSupport.properties(10), meters, time);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int request = 0; request < 100; request++) {
                results.add(executor.submit(() -> check(service, "same-subject").allowed()));
            }
            long allowed = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    allowed++;
                }
            }
            assertThat(allowed).isEqualTo(10);
        } finally {
            meters.close();
        }
    }

    @Test
    void rejectsNewKeysWhenBoundedCacheCapacityIsReached() {
        RateLimitProperties base = RateLimitTestSupport.properties(10);
        RateLimitProperties bounded = new RateLimitProperties(
                true,
                1,
                base.cacheExpireAfterAccess(),
                false,
                List.of(),
                base.clientAddress(),
                base.subject(),
                base.organization(),
                base.metrics(),
                base.process());
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            RateLimitService service = service(bounded, meters, new MutableTimeMeter());

            assertThat(check(service, "first").allowed()).isTrue();
            RateLimitDecision second = check(service, "second");

            assertThat(second.allowed()).isFalse();
            assertThat(second.outcome()).isEqualTo("capacity_rejected");
            assertThat(service.capacityExceeded()).isTrue();
            assertThat(service.estimatedSize()).isLessThanOrEqualTo(1);
        } finally {
            meters.close();
        }
    }

    @Test
    void expiresInactiveKeysAfterConfiguredDuration() {
        RateLimitProperties base = RateLimitTestSupport.properties(10);
        RateLimitProperties bounded = new RateLimitProperties(
                true,
                1,
                Duration.ofMinutes(10),
                false,
                List.of(),
                base.clientAddress(),
                base.subject(),
                base.organization(),
                base.metrics(),
                base.process());
        MutableTimeMeter time = new MutableTimeMeter();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            RateLimitService service = service(bounded, meters, time);
            assertThat(check(service, "first").allowed()).isTrue();

            time.advance(Duration.ofMinutes(11));
            service.cleanUp();

            assertThat(check(service, "second").allowed()).isTrue();
            assertThat(service.capacityExceeded()).isFalse();
        } finally {
            meters.close();
        }
    }

    private RateLimitDecision check(RateLimitService service, String key) {
        return service.check(
                RateLimitScope.SUBJECT,
                key,
                RateLimitEndpointClass.API_READ);
    }

    private RateLimitService service(
            RateLimitProperties properties,
            SimpleMeterRegistry meters,
            MutableTimeMeter time) {
        return new RateLimitService(
                properties,
                new RateLimitKeyHasher(),
                new RateLimitMetrics(meters),
                time);
    }
}
