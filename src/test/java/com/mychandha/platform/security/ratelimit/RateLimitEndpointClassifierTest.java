package com.mychandha.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimitEndpointClassifierTest {

    private final RateLimitEndpointClassifier classifier =
            new RateLimitEndpointClassifier();

    @Test
    void classifiesOnlyBoundedEndpointClasses() {
        assertThat(classify("GET", "/actuator/health/readiness"))
                .isEqualTo(RateLimitEndpointClass.HEALTH);
        assertThat(classify("GET", "/actuator/prometheus"))
                .isEqualTo(RateLimitEndpointClass.METRICS);
        assertThat(classify("GET", "/api/v1/platform/identity"))
                .isEqualTo(RateLimitEndpointClass.API_READ);
        assertThat(classify("POST", "/api/v1/organizations"))
                .isEqualTo(RateLimitEndpointClass.API_COMMAND);
        assertThat(classify("GET", "/unknown"))
                .isEqualTo(RateLimitEndpointClass.OTHER);
    }

    private RateLimitEndpointClass classify(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        return classifier.classify(request);
    }
}
