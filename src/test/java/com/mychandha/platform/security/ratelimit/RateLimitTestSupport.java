package com.mychandha.platform.security.ratelimit;

import java.time.Duration;
import java.util.List;

final class RateLimitTestSupport {

    private RateLimitTestSupport() {
    }

    static RateLimitProperties properties(long capacity) {
        RateLimitProperties.Limit limit =
                new RateLimitProperties.Limit(capacity, Duration.ofMinutes(1));
        return new RateLimitProperties(
                true,
                100,
                Duration.ofMinutes(10),
                false,
                List.of(),
                limit,
                limit,
                limit,
                limit,
                new RateLimitProperties.Limit(2_000, Duration.ofMinutes(1)));
    }

    static RateLimitProperties proxyProperties(List<String> cidrs) {
        RateLimitProperties.Limit limit =
                new RateLimitProperties.Limit(10, Duration.ofMinutes(1));
        return new RateLimitProperties(
                true,
                100,
                Duration.ofMinutes(10),
                true,
                cidrs,
                limit,
                limit,
                limit,
                limit,
                limit);
    }
}
