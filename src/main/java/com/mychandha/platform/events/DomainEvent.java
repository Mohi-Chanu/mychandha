package com.mychandha.platform.events;

import java.util.Map;

public record DomainEvent(
        String aggregateType,
        String aggregateId,
        String eventType,
        int schemaVersion,
        Map<String, Object> payload) {

    public DomainEvent {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
