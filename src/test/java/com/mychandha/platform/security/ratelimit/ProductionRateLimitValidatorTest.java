package com.mychandha.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProductionRateLimitValidatorTest {

    private final io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
            new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

    @AfterEach
    void closeRegistry() {
        registry.close();
    }

    @Test
    void rejectsDisabledLimiterAndMissingProxyBoundary() {
        RateLimitProperties enabledWithoutProxy = RateLimitTestSupport.properties(10);
        RateLimitProperties disabled =
                copy(false, ClientAddressStrategy.DIRECT, List.of());

        assertThatThrownBy(() -> validator(disabled).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be enabled");
        assertThatThrownBy(() -> validator(enabledWithoutProxy).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("client-address boundary");
    }

    @Test
    void acceptsEnabledLimiterWithExplicitTrustedProxyCidr() {
        assertThatCode(() -> validator(copy(
                                true,
                                ClientAddressStrategy.TRUSTED_PROXY_CIDR,
                                List.of("10.0.0.0/8")))
                        .validate())
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsRenderBoundaryWithoutGuessedCidrs() {
        assertThatCode(() -> validator(copy(
                                true,
                                ClientAddressStrategy.RENDER_EDGE_FIRST_HOP,
                                List.of()))
                        .validate())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsRenderBoundaryMixedWithCidrs() {
        assertThatThrownBy(() -> validator(copy(
                                true,
                                ClientAddressStrategy.RENDER_EDGE_FIRST_HOP,
                                List.of("0.0.0.0/0")))
                        .validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("client-address boundary");
    }

    private ProductionRateLimitValidator validator(RateLimitProperties properties) {
        return new ProductionRateLimitValidator(
                properties,
                new ClientAddressResolver(
                        properties,
                        new RateLimitMetrics(registry)));
    }

    private RateLimitProperties copy(
            boolean enabled,
            ClientAddressStrategy strategy,
            List<String> cidrs) {
        RateLimitProperties.Limit limit =
                new RateLimitProperties.Limit(10, Duration.ofMinutes(1));
        return new RateLimitProperties(
                enabled,
                100,
                Duration.ofMinutes(10),
                strategy,
                cidrs,
                limit,
                limit,
                limit,
                limit,
                limit);
    }
}
