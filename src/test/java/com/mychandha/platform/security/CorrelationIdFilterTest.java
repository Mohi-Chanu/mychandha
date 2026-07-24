package com.mychandha.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

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

    private void assertThatCodeIsUuid(String value) {
        assertThat(UUID.fromString(value)).isNotNull();
    }
}
