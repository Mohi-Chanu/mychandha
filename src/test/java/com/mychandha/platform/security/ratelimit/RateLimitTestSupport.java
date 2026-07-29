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
                ClientAddressStrategy.DIRECT,
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
                ClientAddressStrategy.TRUSTED_PROXY_CIDR,
                cidrs,
                limit,
                limit,
                limit,
                limit,
                limit);
    }

    static RateLimitProperties renderProperties() {
        RateLimitProperties.Limit limit =
                new RateLimitProperties.Limit(10, Duration.ofMinutes(1));
        return new RateLimitProperties(
                true,
                100,
                Duration.ofMinutes(10),
                ClientAddressStrategy.RENDER_EDGE_FIRST_HOP,
                List.of(),
                limit,
                limit,
                limit,
                limit,
                limit);
    }
}
