package com.mychandha.platform.observability;

import com.mychandha.platform.events.OutboxProperties;
import java.time.OffsetDateTime;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile({"dispatcher", "local"})
public final class OutboxBacklogReader {

    private final JdbcClient jdbc;
    private final OutboxProperties properties;

    public OutboxBacklogReader(JdbcClient jdbc, OutboxProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    public OutboxBacklog read() {
        return jdbc.sql("""
                        SELECT pending, oldest, retry_attempts,
                               dead_letter, stale_processing
                        FROM platform.outbox_backlog(:lockTimeoutSeconds)
                        """)
                .param("lockTimeoutSeconds", properties.lockTimeout().toSeconds())
                .query((resultSet, rowNumber) -> new OutboxBacklog(
                        resultSet.getLong("pending"),
                        resultSet.getObject("oldest", OffsetDateTime.class),
                        resultSet.getLong("retry_attempts"),
                        resultSet.getLong("dead_letter"),
                        resultSet.getLong("stale_processing")))
                .single();
    }
}
