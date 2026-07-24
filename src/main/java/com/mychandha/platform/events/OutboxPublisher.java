package com.mychandha.platform.events;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final EventTransport transport;
    private final OutboxProperties properties;
    private final String workerId = UUID.randomUUID().toString();
    private final Counter published;
    private final Counter failed;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Dependencies are container-managed infrastructure collaborators.")
    public OutboxPublisher(
            JdbcClient jdbc,
            TransactionTemplate transactions,
            EventTransport transport,
            OutboxProperties properties,
            MeterRegistry metrics) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.transport = transport;
        this.properties = properties;
        this.published = metrics.counter("mychandha.outbox.published");
        this.failed = metrics.counter("mychandha.outbox.failed");
    }

    @Scheduled(fixedDelayString = "${mychandha.outbox.poll-delay:PT1S}")
    public void publishAvailable() {
        claim().forEach(this::publishOne);
    }

    List<OutboxEnvelope> claim() {
        return transactions.execute(status -> jdbc.sql("""
                        WITH candidates AS (
                            SELECT id
                            FROM platform.outbox_event
                            WHERE (status = 'PENDING' AND available_at <= now())
                               OR (status = 'PROCESSING'
                                   AND locked_at < now()
                                       - (:lockTimeoutSeconds * interval '1 second'))
                            ORDER BY created_at
                            FOR UPDATE SKIP LOCKED
                            LIMIT :batchSize
                        )
                        UPDATE platform.outbox_event event
                        SET status = 'PROCESSING',
                            attempts = attempts + 1,
                            locked_at = now(),
                            locked_by = :workerId
                        FROM candidates
                        WHERE event.id = candidates.id
                        RETURNING event.id, event.organization_id, event.aggregate_type,
                                  event.aggregate_id, event.event_type, event.schema_version,
                                  event.payload::text, event.correlation_id, event.attempts
                        """)
                .param("batchSize", properties.batchSize())
                .param("lockTimeoutSeconds", properties.lockTimeout().toSeconds())
                .param("workerId", workerId)
                .query(this::mapEnvelope)
                .list());
    }

    private void publishOne(OutboxEnvelope event) {
        try (MDC.MDCCloseable correlation = MDC.putCloseable(
                     "correlationId", java.util.Objects.toString(event.correlationId(), ""));
             MDC.MDCCloseable organization = MDC.putCloseable(
                     "organizationId", event.organizationId().toString())) {
            transport.publish(event);
            transactions.executeWithoutResult(status -> jdbc.sql("""
                            UPDATE platform.outbox_event
                            SET status = 'PUBLISHED', published_at = now(),
                                locked_at = NULL, locked_by = NULL, last_error_code = NULL
                            WHERE id = :id AND status = 'PROCESSING' AND locked_by = :workerId
                            """)
                    .param("id", event.id())
                    .param("workerId", workerId)
                    .update());
            published.increment();
        } catch (RuntimeException exception) {
            reschedule(event, exception);
        }
    }

    private void reschedule(OutboxEnvelope event, RuntimeException exception) {
        boolean deadLetter = event.attempts() >= properties.maxAttempts();
        Duration backoff = Duration.ofSeconds(Math.min(3600, 1L << Math.min(event.attempts(), 11)));
        transactions.executeWithoutResult(status -> jdbc.sql("""
                        UPDATE platform.outbox_event
                        SET status = :status,
                            available_at = now() + (:backoffSeconds * interval '1 second'),
                            locked_at = NULL,
                            locked_by = NULL,
                            last_error_code = :errorCode
                        WHERE id = :id AND status = 'PROCESSING' AND locked_by = :workerId
                        """)
                .param("status", deadLetter ? "DEAD_LETTER" : "PENDING")
                .param("backoffSeconds", backoff.toSeconds())
                .param("errorCode", exception.getClass().getSimpleName())
                .param("id", event.id())
                .param("workerId", workerId)
                .update());
        failed.increment();
        log.warn("Outbox delivery failed eventId={} eventType={} attempt={} deadLetter={}",
                event.id(), event.eventType(), event.attempts(), deadLetter);
    }

    private OutboxEnvelope mapEnvelope(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OutboxEnvelope(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("organization_id", UUID.class),
                resultSet.getString("aggregate_type"),
                resultSet.getString("aggregate_id"),
                resultSet.getString("event_type"),
                resultSet.getInt("schema_version"),
                resultSet.getString("payload"),
                resultSet.getString("correlation_id"),
                resultSet.getInt("attempts"));
    }
}
