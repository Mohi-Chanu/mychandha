package com.mychandha.platform.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychandha.platform.tenancy.OrganizationContext;
import com.mychandha.platform.tenancy.TenantJdbcExecutor;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private final TenantJdbcExecutor tenantJdbc;
    private final ObjectMapper objectMapper;

    public OutboxService(TenantJdbcExecutor tenantJdbc, ObjectMapper objectMapper) {
        this.tenantJdbc = tenantJdbc;
        this.objectMapper = objectMapper;
    }

    public UUID enqueue(DomainEvent event) {
        return tenantJdbc.write(jdbc -> enqueue(jdbc, event));
    }

    /**
     * Call this overload from the same TenantJdbcExecutor transaction as the
     * aggregate mutation to preserve the transactional-outbox invariant.
     */
    public UUID enqueue(JdbcClient jdbc, DomainEvent event) {
        UUID eventId = UUID.randomUUID();
        jdbc.sql("""
                        INSERT INTO platform.outbox_event (
                            id, organization_id, aggregate_type, aggregate_id,
                            event_type, schema_version, payload, correlation_id
                        ) VALUES (
                            :id, :organizationId, :aggregateType, :aggregateId,
                            :eventType, :schemaVersion, CAST(:payload AS jsonb), :correlationId
                        )
                        """)
                .param("id", eventId)
                .param("organizationId", OrganizationContext.require().organizationId())
                .param("aggregateType", event.aggregateType())
                .param("aggregateId", event.aggregateId())
                .param("eventType", event.eventType())
                .param("schemaVersion", event.schemaVersion())
                .param("payload", json(event))
                .param("correlationId", MDC.get("correlationId"))
                .update();
        return eventId;
    }

    private String json(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event.payload());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Domain event payload could not be serialized", exception);
        }
    }
}
