package com.mychandha.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitProblemWriterTest {

    @Test
    void writesStableProblemWithoutIdentityMaterial() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", "safe-correlation")) {
            new RateLimitProblemWriter(new ObjectMapper()).write(
                    response,
                    RateLimitDecision.rejected(
                            12,
                            RateLimitScope.SUBJECT,
                            RateLimitEndpointClass.API_COMMAND,
                            "limited"));
        }

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("12");
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"code\":\"RATE_LIMIT_EXCEEDED\"")
                .contains("\"correlationId\":\"safe-correlation\"")
                .doesNotContain("subject")
                .doesNotContain("organization");
    }
}
