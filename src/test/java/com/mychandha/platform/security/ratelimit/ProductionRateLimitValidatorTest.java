package com.mychandha.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductionRateLimitValidatorTest {

    @Test
    void rejectsDisabledLimiterAndMissingProxyBoundary() {
        RateLimitProperties enabledWithoutProxy = RateLimitTestSupport.properties(10);
        RateLimitProperties disabled = copy(false, false, List.of());

        assertThatThrownBy(() -> validator(disabled).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be enabled");
        assertThatThrownBy(() -> validator(enabledWithoutProxy).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trusted forwarded-address");
    }

    @Test
    void acceptsEnabledLimiterWithExplicitTrustedProxyCidr() {
        assertThatCode(() -> validator(copy(true, true, List.of("10.0.0.0/8"))).validate())
                .doesNotThrowAnyException();
    }

    private ProductionRateLimitValidator validator(RateLimitProperties properties) {
        return new ProductionRateLimitValidator(
                properties,
                new TrustedProxyClientAddressResolver(properties));
    }

    private RateLimitProperties copy(
            boolean enabled,
            boolean trustForwarded,
            List<String> cidrs) {
        RateLimitProperties.Limit limit =
                new RateLimitProperties.Limit(10, Duration.ofMinutes(1));
        return new RateLimitProperties(
                enabled,
                100,
                Duration.ofMinutes(10),
                trustForwarded,
                cidrs,
                limit,
                limit,
                limit,
                limit,
                limit);
    }
}
