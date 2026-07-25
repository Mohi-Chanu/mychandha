package com.mychandha.platform.observability;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("durableDelivery")
@Profile({"dispatcher", "local"})
public final class OutboxHealthIndicator implements HealthIndicator {

    private static final long UNHEALTHY_AGE_SECONDS = 300;

    private final OutboxBacklogReader backlogReader;

    public OutboxHealthIndicator(OutboxBacklogReader backlogReader) {
        this.backlogReader = backlogReader;
    }

    @Override
    public Health health() {
        OutboxBacklog backlog = backlogReader.read();
        long oldestAgeSeconds = backlog.oldest() == null
                ? 0
                : Math.max(0, Duration.between(
                        backlog.oldest(), OffsetDateTime.now()).toSeconds());
        Health.Builder builder = oldestAgeSeconds > UNHEALTHY_AGE_SECONDS
                || backlog.staleProcessing() > 0
                ? Health.down()
                : Health.up();
        return builder
                .withDetail("pending", backlog.pending())
                .withDetail("oldestAgeSeconds", oldestAgeSeconds)
                .withDetail("deadLetter", backlog.deadLetter())
                .withDetail("staleProcessing", backlog.staleProcessing())
                .build();
    }
}
