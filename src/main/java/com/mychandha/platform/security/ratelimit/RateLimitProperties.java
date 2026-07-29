package com.mychandha.platform.security.ratelimit;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mychandha.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int cacheMaximumSize,
        Duration cacheExpireAfterAccess,
        ClientAddressStrategy clientAddressStrategy,
        List<String> trustedProxyCidrs,
        Limit clientAddress,
        Limit subject,
        Limit organization,
        Limit metrics,
        Limit process) {

    public RateLimitProperties {
        if (cacheMaximumSize <= 0) {
            throw new IllegalArgumentException("Rate-limit cache maximum size must be positive");
        }
        if (cacheExpireAfterAccess == null
                || cacheExpireAfterAccess.isZero()
                || cacheExpireAfterAccess.isNegative()) {
            throw new IllegalArgumentException("Rate-limit cache expiry must be positive");
        }
        if (clientAddressStrategy == null) {
            throw new IllegalArgumentException(
                    "Rate-limit client-address strategy is required");
        }
        trustedProxyCidrs = trustedProxyCidrs == null
                ? List.of()
                : List.copyOf(trustedProxyCidrs.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .toList());
        requireLimit("client-address", clientAddress);
        requireLimit("subject", subject);
        requireLimit("organization", organization);
        requireLimit("metrics", metrics);
        requireLimit("process", process);
    }

    Limit limitFor(RateLimitScope scope) {
        return switch (scope) {
            case CLIENT_ADDRESS -> clientAddress;
            case SUBJECT -> subject;
            case ORGANIZATION -> organization;
            case METRICS -> metrics;
            case PROCESS -> process;
        };
    }

    @Override
    public List<String> trustedProxyCidrs() {
        return List.copyOf(trustedProxyCidrs);
    }

    private static void requireLimit(String name, Limit limit) {
        if (limit == null) {
            throw new IllegalArgumentException("Missing rate-limit policy: " + name);
        }
    }

    public record Limit(long capacity, Duration refillPeriod) {

        public Limit {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Rate-limit capacity must be positive");
            }
            if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
                throw new IllegalArgumentException("Rate-limit refill period must be positive");
            }
        }
    }
}
