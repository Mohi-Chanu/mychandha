package com.mychandha.platform.security.ratelimit;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("rateLimit")
@Profile({"api", "local", "test"})
public final class RateLimitHealthIndicator implements HealthIndicator {

    private final RateLimitService rateLimits;
    private final ClientAddressResolver addressResolver;

    RateLimitHealthIndicator(
            RateLimitService rateLimits,
            ClientAddressResolver addressResolver) {
        this.rateLimits = rateLimits;
        this.addressResolver = addressResolver;
    }

    @Override
    public Health health() {
        Health.Builder health = rateLimits.enabled()
                        && !rateLimits.capacityExceeded()
                        && !addressResolver.forwardingDegraded()
                ? Health.up()
                : Health.down();
        return health
                .withDetail("enabled", rateLimits.enabled())
                .withDetail("entries", rateLimits.estimatedSize())
                .withDetail("maximumEntries", rateLimits.maximumSize())
                .withDetail(
                        "forwardingAnomalies",
                        addressResolver.consecutiveForwardingAnomalies())
                .build();
    }
}
