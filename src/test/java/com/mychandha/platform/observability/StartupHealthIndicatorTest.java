package com.mychandha.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class StartupHealthIndicatorTest {

    @Test
    void startupIsDistinctFromLivenessAndChangesOnlyAfterReadyEvent() {
        StartupHealthIndicator indicator = new StartupHealthIndicator();

        assertThat(indicator.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);

        indicator.onApplicationEvent(null);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }
}
