package com.mychandha.platform.events;

import java.util.UUID;

public record OutboxEnvelope(
        UUID id,
        UUID organizationId,
        String aggregateType,
        String aggregateId,
        String eventType,
        int schemaVersion,
        String payload,
        String correlationId,
        int attempts) {
}
