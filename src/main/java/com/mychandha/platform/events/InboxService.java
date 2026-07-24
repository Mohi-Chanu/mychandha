package com.mychandha.platform.events;

import com.mychandha.platform.tenancy.OrganizationContext;
import com.mychandha.platform.tenancy.TenantJdbcExecutor;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class InboxService {

    private final TenantJdbcExecutor tenantJdbc;

    public InboxService(TenantJdbcExecutor tenantJdbc) {
        this.tenantJdbc = tenantJdbc;
    }

    /**
     * @return true when the external event is new; false for an exact replay.
     * @throws IllegalStateException if an event id is replayed with new content.
     */
    public boolean receive(String source, String externalEventId, byte[] rawPayload) {
        String payloadHash = sha256(rawPayload);
        return tenantJdbc.write(jdbc -> {
            var existingHash = jdbc.sql("""
                            SELECT payload_hash
                            FROM platform.inbox_event
                            WHERE source = :source AND external_event_id = :externalEventId
                            """)
                    .param("source", source)
                    .param("externalEventId", externalEventId)
                    .query(String.class)
                    .optional();
            if (existingHash.isPresent()) {
                if (!existingHash.get().equals(payloadHash)) {
                    throw new IllegalStateException("External event id was reused with different content");
                }
                return false;
            }
            jdbc.sql("""
                            INSERT INTO platform.inbox_event (
                                organization_id, source, external_event_id, payload_hash
                            ) VALUES (:organizationId, :source, :externalEventId, :payloadHash)
                            """)
                    .param("organizationId", OrganizationContext.require().organizationId())
                    .param("source", source)
                    .param("externalEventId", externalEventId)
                    .param("payloadHash", payloadHash)
                    .update();
            return true;
        });
    }

    static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
