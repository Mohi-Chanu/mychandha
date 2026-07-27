package com.mychandha.platform.security.ratelimit;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production & api")
final class ProductionRateLimitValidator {

    private final RateLimitProperties properties;
    private final TrustedProxyClientAddressResolver addressResolver;

    ProductionRateLimitValidator(
            RateLimitProperties properties,
            TrustedProxyClientAddressResolver addressResolver) {
        this.properties = properties;
        this.addressResolver = addressResolver;
    }

    @PostConstruct
    void validate() {
        if (!properties.enabled()) {
            throw new IllegalStateException(
                    "Application rate limiting must be enabled for production API runtime");
        }
        if (!addressResolver.isForwardedBoundaryConfigured()) {
            throw new IllegalStateException(
                    "Production API requires a trusted forwarded-address boundary");
        }
    }
}
