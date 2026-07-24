package com.mychandha.platform.audit;

import com.mychandha.platform.tenancy.OrganizationContext;
import com.mychandha.platform.tenancy.TenantJdbcExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditIntegrityService {

    private final TenantJdbcExecutor tenantJdbc;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "TenantJdbcExecutor is a container-managed stateless collaborator.")
    public AuditIntegrityService(TenantJdbcExecutor tenantJdbc) {
        this.tenantJdbc = tenantJdbc;
    }

    public VerificationResult verifyCurrentOrganization() {
        UUID organizationId = OrganizationContext.require().organizationId();
        List<StoredEvent> events = tenantJdbc.read(jdbc -> jdbc.sql("""
                        SELECT id, organization_id, sequence_number, event_type,
                               actor_provider, actor_subject, resource_type, resource_id,
                               recorded_at, event_data_canonical, previous_hash, event_hash
                        FROM audit.audit_event
                        WHERE organization_id = :organizationId
                        ORDER BY sequence_number
                        """)
                .param("organizationId", organizationId)
                .query(this::mapEvent)
                .list());

        long expectedSequence = 1;
        String previousHash = null;
        for (StoredEvent event : events) {
            if (event.sequenceNumber() != expectedSequence) {
                return VerificationResult.invalid(
                        events.size(), event.id(), "SEQUENCE_GAP");
            }
            if (!Objects.equals(event.previousHash(), previousHash)) {
                return VerificationResult.invalid(
                        events.size(), event.id(), "PREVIOUS_HASH_MISMATCH");
            }
            String calculated = AuditService.eventHash(new AuditService.HashMaterial(
                    event.id(),
                    event.organizationId(),
                    event.sequenceNumber(),
                    event.eventType(),
                    event.actorProvider(),
                    event.actorSubject(),
                    event.resourceType(),
                    event.resourceId(),
                    event.recordedAt().toInstant(),
                    event.canonicalData(),
                    event.previousHash()));
            if (!calculated.equals(event.eventHash())) {
                return VerificationResult.invalid(
                        events.size(), event.id(), "EVENT_HASH_MISMATCH");
            }
            previousHash = event.eventHash();
            expectedSequence++;
        }
        return VerificationResult.valid(events.size(), previousHash);
    }

    private StoredEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StoredEvent(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("organization_id", UUID.class),
                resultSet.getLong("sequence_number"),
                resultSet.getString("event_type"),
                resultSet.getString("actor_provider"),
                resultSet.getString("actor_subject"),
                resultSet.getString("resource_type"),
                resultSet.getString("resource_id"),
                resultSet.getObject("recorded_at", OffsetDateTime.class),
                resultSet.getString("event_data_canonical"),
                resultSet.getString("previous_hash"),
                resultSet.getString("event_hash"));
    }

    public record VerificationResult(
            boolean valid,
            int eventCount,
            String lastHash,
            UUID failingEventId,
            String failureCode) {

        static VerificationResult valid(int eventCount, String lastHash) {
            return new VerificationResult(true, eventCount, lastHash, null, null);
        }

        static VerificationResult invalid(
                int eventCount,
                UUID failingEventId,
                String failureCode) {
            return new VerificationResult(
                    false, eventCount, null, failingEventId, failureCode);
        }
    }

    private record StoredEvent(
            UUID id,
            UUID organizationId,
            long sequenceNumber,
            String eventType,
            String actorProvider,
            String actorSubject,
            String resourceType,
            String resourceId,
            OffsetDateTime recordedAt,
            String canonicalData,
            String previousHash,
            String eventHash) {
    }
}
