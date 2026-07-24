package com.mychandha.platform.events;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Launch transport. Event contracts remain broker-neutral, so Kafka or another
 * durable transport can replace this adapter without changing domain modules.
 */
@Component
public final class InProcessEventTransport implements EventTransport {

    private final List<DomainEventConsumer> consumers;

    public InProcessEventTransport(List<DomainEventConsumer> consumers) {
        this.consumers = List.copyOf(consumers);
    }

    @Override
    public void publish(OutboxEnvelope event) {
        List<DomainEventConsumer> matching = consumers.stream()
                .filter(consumer -> consumer.supports(event.eventType(), event.schemaVersion()))
                .toList();
        if (matching.isEmpty()) {
            throw new UnsupportedOperationException(
                    "No consumer registered for " + event.eventType() + " v" + event.schemaVersion());
        }
        matching.forEach(consumer -> consumer.accept(event));
    }
}
