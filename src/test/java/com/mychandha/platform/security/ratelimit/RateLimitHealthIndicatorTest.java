package com.mychandha.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimitHealthIndicatorTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @AfterEach
    void closeRegistry() {
        registry.close();
    }

    @Test
    void degradesAfterRepeatedRenderHeaderAnomaliesAndRecovers() {
        RateLimitProperties properties = RateLimitTestSupport.renderProperties();
        RateLimitMetrics metrics = new RateLimitMetrics(registry);
        ClientAddressResolver resolver = new ClientAddressResolver(properties, metrics);
        RateLimitService service =
                new RateLimitService(properties, new RateLimitKeyHasher(), metrics);
        RateLimitHealthIndicator indicator =
                new RateLimitHealthIndicator(service, resolver);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");

        resolver.resolve(request);
        resolver.resolve(request);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);

        resolver.resolve(request);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails())
                .containsEntry("forwardingAnomalies", 3);

        request.addHeader(ClientAddressResolver.FORWARDED_FOR, "203.0.113.7");
        resolver.resolve(request);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }
}
