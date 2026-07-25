package com.mychandha.platform.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"dispatcher", "local"})
public final class OutboxMetrics {

    private final OutboxBacklogReader backlogReader;
    private final Counter published;
    private final Counter failed;
    private final Counter retried;
    private final Counter reclaimed;
    private final Counter deadLettered;
    private final Timer delivery;
    private final AtomicLong pending = new AtomicLong();
    private final AtomicLong oldestAgeSeconds = new AtomicLong();
    private final AtomicLong retryAttempts = new AtomicLong();
    private final AtomicLong deadLetter = new AtomicLong();
    private final AtomicLong staleProcessing = new AtomicLong();

    public OutboxMetrics(MeterRegistry registry, OutboxBacklogReader backlogReader) {
        this.backlogReader = backlogReader;
        this.published = registry.counter("mychandha.outbox.published");
        this.failed = registry.counter("mychandha.outbox.failed");
        this.retried = registry.counter("mychandha.outbox.retried");
        this.reclaimed = registry.counter("mychandha.outbox.stale.recovered");
        this.deadLettered = registry.counter("mychandha.outbox.dead.lettered");
        this.delivery = registry.timer("mychandha.outbox.delivery");
        registerGauge(registry, "mychandha.outbox.pending", pending);
        registerGauge(registry, "mychandha.outbox.oldest.age", oldestAgeSeconds);
        registerGauge(registry, "mychandha.outbox.retry.attempts", retryAttempts);
        registerGauge(registry, "mychandha.outbox.dead.letter", deadLetter);
        registerGauge(registry, "mychandha.outbox.stale.processing", staleProcessing);
    }

    @Scheduled(fixedDelayString = "${mychandha.outbox.poll-delay:PT1S}")
    public void refreshBacklog() {
        OutboxBacklog backlog = backlogReader.read();
        pending.set(backlog.pending());
        oldestAgeSeconds.set(ageSeconds(backlog.oldest()));
        retryAttempts.set(backlog.retryAttempts());
        deadLetter.set(backlog.deadLetter());
        staleProcessing.set(backlog.staleProcessing());
    }

    public void claimed(boolean wasReclaimed) {
        if (wasReclaimed) {
            reclaimed.increment();
        }
    }

    public void recordDelivery(Runnable action) {
        delivery.record(action);
    }

    public void published() {
        published.increment();
    }

    public void failed(boolean isDeadLetter) {
        failed.increment();
        if (isDeadLetter) {
            deadLettered.increment();
        } else {
            retried.increment();
        }
    }

    private void registerGauge(
            MeterRegistry registry,
            String name,
            AtomicLong value) {
        Gauge.builder(name, value, AtomicLong::get).register(registry);
    }

    private long ageSeconds(OffsetDateTime timestamp) {
        return timestamp == null
                ? 0
                : Math.max(0, Duration.between(timestamp, OffsetDateTime.now()).toSeconds());
    }
}
