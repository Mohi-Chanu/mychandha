package com.mychandha.platform.events;

public interface DomainEventConsumer {

    boolean supports(String eventType, int schemaVersion);

    void accept(OutboxEnvelope event);
}
