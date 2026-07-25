package com.mychandha.platform.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OutboxSqlBoundaryTest {

    @Test
    void javaRuntimeDoesNotContainCrossTenantOutboxStatements() throws IOException {
        Path source = Path.of(
                "src/main/java/com/mychandha/platform/events/OutboxPublisher.java");
        String content = Files.readString(source);

        assertThat(content)
                .doesNotContain("FOR UPDATE SKIP LOCKED")
                .doesNotContain("UPDATE platform.outbox_event")
                .contains("platform.claim_outbox_events")
                .contains("platform.mark_outbox_published")
                .contains("platform.reschedule_outbox_event");
    }
}
