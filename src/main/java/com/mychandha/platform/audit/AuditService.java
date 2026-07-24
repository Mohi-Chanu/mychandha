package com.mychandha.platform.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychandha.platform.tenancy.OrganizationContext;
import com.mychandha.platform.tenancy.TenantJdbcExecutor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final TenantJdbcExecutor tenantJdbc;
    private final ObjectMapper objectMapper;

    public AuditService(TenantJdbcExecutor tenantJdbc, ObjectMapper objectMapper) {
        this.tenantJdbc = tenantJdbc;
        this.objectMapper = objectMapper;
    }

    public UUID append(AuditEvent event) {
        OrganizationContext.Scope scope = OrganizationContext.require();
        UUID eventId = UUID.randomUUID();
        Instant recordedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        String canonicalData = canonicalJson(event.data());

        return tenantJdbc.write(jdbc -> {
            jdbc.sql("SELECT pg_advisory_xact_lock(hashtextextended(:organizationId, 0)) AS lock")
                    .param("organizationId", scope.organizationId().toString())
                    .query()
                    .singleRow();
            PreviousEvent previous = jdbc.sql("""
                            SELECT sequence_number, event_hash
                            FROM audit.audit_event
                            WHERE organization_id = :organizationId
                            ORDER BY sequence_number DESC
                            LIMIT 1
                            """)
                    .param("organizationId", scope.organizationId())
                    .query((resultSet, rowNumber) -> new PreviousEvent(
                            resultSet.getLong("sequence_number"),
                            resultSet.getString("event_hash")))
                    .optional()
                    .orElse(new PreviousEvent(0, ""));
            long sequenceNumber = previous.sequenceNumber() + 1;
            String eventHash = eventHash(new HashMaterial(
                    eventId,
                    scope.organizationId(),
                    sequenceNumber,
                    event.eventType(),
                    scope.actor().provider(),
                    scope.actor().subject(),
                    event.resourceType(),
                    event.resourceId(),
                    recordedAt,
                    canonicalData,
                    previous.eventHash()));

            jdbc.sql("""
                            INSERT INTO audit.audit_event (
                                id, organization_id, sequence_number, event_type, actor_provider, actor_subject,
                                resource_type, resource_id, correlation_id, event_data,
                                event_data_canonical,
                                previous_hash, event_hash, recorded_at
                            ) VALUES (
                                :id, :organizationId, :sequenceNumber, :eventType, :actorProvider, :actorSubject,
                                :resourceType, :resourceId, :correlationId, CAST(:eventData AS jsonb),
                                :eventDataCanonical,
                                :previousHash, :eventHash, :recordedAt
                            )
                            """)
                    .param("id", eventId)
                    .param("organizationId", scope.organizationId())
                    .param("sequenceNumber", sequenceNumber)
                    .param("eventType", event.eventType())
                    .param("actorProvider", scope.actor().provider())
                    .param("actorSubject", scope.actor().subject())
                    .param("resourceType", event.resourceType())
                    .param("resourceId", event.resourceId())
                    .param("correlationId", MDC.get("correlationId"))
                    .param("eventData", canonicalData)
                    .param("eventDataCanonical", canonicalData)
                    .param("previousHash", previous.eventHash().isBlank() ? null : previous.eventHash())
                    .param("eventHash", eventHash)
                    .param("recordedAt", OffsetDateTime.ofInstant(recordedAt, ZoneOffset.UTC))
                    .update();
            return eventId;
        });
    }

    String canonicalJson(Map<String, Object> data) {
        try {
            return objectMapper.copy()
                    .configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                    .writeValueAsString(data == null ? Map.of() : data);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Audit data could not be serialized", exception);
        }
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    static String eventHash(HashMaterial material) {
        return sha256(String.join("|",
                java.util.Objects.toString(material.previousHash(), ""),
                material.eventId().toString(),
                material.organizationId().toString(),
                Long.toString(material.sequenceNumber()),
                material.eventType(),
                material.actorProvider(),
                material.actorSubject(),
                material.resourceType(),
                java.util.Objects.toString(material.resourceId(), ""),
                material.recordedAt().toString(),
                material.canonicalData()));
    }

    record HashMaterial(
            UUID eventId,
            UUID organizationId,
            long sequenceNumber,
            String eventType,
            String actorProvider,
            String actorSubject,
            String resourceType,
            String resourceId,
            Instant recordedAt,
            String canonicalData,
            String previousHash) {
    }

    private record PreviousEvent(long sequenceNumber, String eventHash) {
    }
}
