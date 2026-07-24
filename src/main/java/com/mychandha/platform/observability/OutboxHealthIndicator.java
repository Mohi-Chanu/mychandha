package com.mychandha.platform.observability;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component("durableDelivery")
public class OutboxHealthIndicator implements HealthIndicator {

    private final JdbcClient jdbc;

    public OutboxHealthIndicator(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Health health() {
        Backlog backlog = jdbc.sql("""
                        SELECT count(*) AS pending,
                               min(created_at) AS oldest
                        FROM platform.outbox_event
                        WHERE status IN ('PENDING', 'PROCESSING')
                        """)
                .query((resultSet, rowNumber) -> new Backlog(
                        resultSet.getLong("pending"),
                        resultSet.getObject("oldest", OffsetDateTime.class)))
                .single();
        long oldestAgeSeconds = backlog.oldest() == null
                ? 0
                : Math.max(0, Duration.between(backlog.oldest(), OffsetDateTime.now()).toSeconds());
        Health.Builder builder = oldestAgeSeconds > 300 ? Health.down() : Health.up();
        return builder
                .withDetail("pending", backlog.pending())
                .withDetail("oldestAgeSeconds", oldestAgeSeconds)
                .build();
    }

    private record Backlog(long pending, OffsetDateTime oldest) {
    }
}
