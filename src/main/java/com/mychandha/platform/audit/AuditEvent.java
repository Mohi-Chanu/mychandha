package com.mychandha.platform.audit;

import java.util.Map;

public record AuditEvent(
        String eventType,
        String resourceType,
        String resourceId,
        Map<String, Object> data) {

    public AuditEvent {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
