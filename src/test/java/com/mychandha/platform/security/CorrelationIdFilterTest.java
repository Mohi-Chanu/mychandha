package com.mychandha.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    @Test
    void preservesSafeCorrelationId() {
        assertThat(CorrelationIdFilter.safeCorrelationId("request-123:attempt_2"))
                .isEqualTo("request-123:attempt_2");
    }

    @Test
    void replacesHeaderInjectionAttempt() {
        String correlationId =
                CorrelationIdFilter.safeCorrelationId("request-123\r\nX-Injected: true");

        assertThat(correlationId).isNotEqualTo("request-123\r\nX-Injected: true");
        assertThatCodeIsUuid(correlationId);
    }

    @Test
    void replacesOversizedCorrelationId() {
        String correlationId = CorrelationIdFilter.safeCorrelationId("a".repeat(101));

        assertThatCodeIsUuid(correlationId);
    }

    @Test
    void preservesValidW3cTraceContext() {
        String traceParent =
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        assertThat(CorrelationIdFilter.safeTraceParent(traceParent))
                .isEqualTo(traceParent);
    }

    @Test
    void replacesInvalidW3cTraceContext() {
        String traceParent = CorrelationIdFilter.safeTraceParent(
                "00-00000000000000000000000000000000-0000000000000000-01");

        assertThat(traceParent)
                .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-01");
    }

    @Test
    void createsSeparateRequestCorrelationAndTraceContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "workflow-123");
        request.addHeader(
                CorrelationIdFilter.TRACE_PARENT_HEADER,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CorrelationIdFilter().doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get("correlationId")).isEqualTo("workflow-123");
            assertThat(MDC.get("requestId")).isNotEqualTo("workflow-123");
            assertThat(MDC.get("traceId"))
                    .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isEqualTo("workflow-123");
        assertThatCodeIsUuid(response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER));
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("traceId")).isNull();
    }

    private void assertThatCodeIsUuid(String value) {
        assertThat(UUID.fromString(value)).isNotNull();
    }
}
