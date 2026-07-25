package com.mychandha.platform.events;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.mychandha.platform.observability.OutboxMetrics;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile({"dispatcher", "local"})
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final EventTransport transport;
    private final OutboxProperties properties;
    private final String workerId = UUID.randomUUID().toString();
    private final OutboxMetrics metrics;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Dependencies are container-managed infrastructure collaborators.")
    public OutboxPublisher(
            JdbcClient jdbc,
            TransactionTemplate transactions,
            EventTransport transport,
            OutboxProperties properties,
            OutboxMetrics metrics) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.transport = transport;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${mychandha.outbox.poll-delay:PT1S}")
    public void publishAvailable() {
        claim().forEach(this::publishOne);
    }

    List<OutboxEnvelope> claim() {
        List<OutboxEnvelope> claimed = Objects.requireNonNull(
                transactions.execute(status -> jdbc.sql("""
                        SELECT *
                        FROM platform.claim_outbox_events(
                            :workerId, :batchSize, :lockTimeoutSeconds
                        )
                        """)
                .param("batchSize", properties.batchSize())
                .param("lockTimeoutSeconds", properties.lockTimeout().toSeconds())
                .param("workerId", workerId)
                .query(this::mapEnvelope)
                .list()),
                "Outbox claim transaction returned no result");
        claimed.forEach(event -> metrics.claimed(event.reclaimed()));
        return claimed;
    }

    private void publishOne(OutboxEnvelope event) {
        try (MDC.MDCCloseable correlation = MDC.putCloseable(
                     "correlationId", Objects.toString(event.correlationId(), ""));
             MDC.MDCCloseable traceParent = MDC.putCloseable(
                     "traceparent", Objects.toString(event.traceParent(), ""));
             MDC.MDCCloseable trace = MDC.putCloseable(
                     "traceId", tracePart(event.traceParent(), 3, 35));
             MDC.MDCCloseable span = MDC.putCloseable(
                     "spanId", tracePart(event.traceParent(), 36, 52));
             MDC.MDCCloseable organization = MDC.putCloseable(
                     "organizationId", event.organizationId().toString())) {
            metrics.recordDelivery(() -> transport.publish(event));
            Boolean transitioned = transactions.execute(status -> jdbc.sql("""
                            SELECT platform.mark_outbox_published(:id, :workerId)
                            """)
                    .param("id", event.id())
                    .param("workerId", workerId)
                    .query(Boolean.class)
                    .single());
            requireTransition(transitioned);
            metrics.published();
        } catch (RuntimeException exception) {
            reschedule(event, exception);
        }
    }

    private void reschedule(OutboxEnvelope event, RuntimeException exception) {
        boolean deadLetter = event.attempts() >= properties.maxAttempts();
        Duration backoff = Duration.ofSeconds(Math.min(3600, 1L << Math.min(event.attempts(), 11)));
        Boolean transitioned = transactions.execute(status -> jdbc.sql("""
                        SELECT platform.reschedule_outbox_event(
                            :id, :workerId, :backoffSeconds, :errorCode, :deadLetter
                        )
                        """)
                .param("backoffSeconds", backoff.toSeconds())
                .param("errorCode", exception.getClass().getSimpleName())
                .param("id", event.id())
                .param("workerId", workerId)
                .param("deadLetter", deadLetter)
                .query(Boolean.class)
                .single());
        requireTransition(transitioned);
        metrics.failed(deadLetter);
        log.warn("Outbox delivery failed eventId={} eventType={} attempt={} deadLetter={}",
                event.id(), event.eventType(), event.attempts(), deadLetter);
    }

    private void requireTransition(Boolean transitioned) {
        if (!Boolean.TRUE.equals(transitioned)) {
            throw new IllegalStateException("Outbox claim ownership was lost");
        }
    }

    private String tracePart(String traceParent, int start, int end) {
        return traceParent == null || traceParent.length() < end
                ? ""
                : traceParent.substring(start, end);
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
                resultSet.getString("trace_parent"),
                resultSet.getInt("attempts"),
                resultSet.getBoolean("reclaimed"));
    }
}
