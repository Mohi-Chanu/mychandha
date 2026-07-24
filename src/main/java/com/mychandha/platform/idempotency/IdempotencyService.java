package com.mychandha.platform.idempotency;

import com.mychandha.platform.tenancy.OrganizationContext;
import com.mychandha.platform.tenancy.TenantJdbcExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final TenantJdbcExecutor tenantJdbc;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "TenantJdbcExecutor is a container-managed stateless collaborator.")
    public IdempotencyService(TenantJdbcExecutor tenantJdbc) {
        this.tenantJdbc = tenantJdbc;
    }

    public IdempotentCommandResult execute(
            String idempotencyKey,
            byte[] canonicalRequest,
            Duration retention,
            Supplier<IdempotentCommandResult> command) {
        validateKey(idempotencyKey);
        String requestHash = sha256(canonicalRequest);
        OrganizationContext.Scope scope = OrganizationContext.require();

        return tenantJdbc.write(jdbc -> {
            jdbc.sql("""
                            SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0)) AS lock
                            """)
                    .param("lockKey", String.join("|",
                            scope.organizationId().toString(),
                            scope.actor().provider(),
                            scope.actor().subject(),
                            idempotencyKey))
                    .query()
                    .singleRow();
            var existing = jdbc.sql("""
                            SELECT request_hash, response_status, response_body::text
                            FROM platform.idempotency_record
                            WHERE organization_id = :organizationId
                              AND actor_subject = :actorSubject
                              AND idempotency_key = :idempotencyKey
                              AND expires_at > now()
                            """)
                    .param("organizationId", scope.organizationId())
                    .param("actorSubject", scope.actor().subject())
                    .param("idempotencyKey", idempotencyKey)
                    .query((resultSet, rowNumber) -> new ExistingRecord(
                            resultSet.getString("request_hash"),
                            resultSet.getInt("response_status"),
                            resultSet.getString("response_body")))
                    .optional();
            if (existing.isPresent()) {
                if (!existing.get().requestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException();
                }
                return new IdempotentCommandResult(
                        existing.get().status(), existing.get().body(), true);
            }

            jdbc.sql("""
                            INSERT INTO platform.idempotency_record (
                                organization_id, actor_subject, idempotency_key,
                                request_hash, expires_at
                            ) VALUES (
                                :organizationId, :actorSubject, :idempotencyKey,
                                :requestHash, now() + (:retentionSeconds * interval '1 second')
                            )
                            """)
                    .param("organizationId", scope.organizationId())
                    .param("actorSubject", scope.actor().subject())
                    .param("idempotencyKey", idempotencyKey)
                    .param("requestHash", requestHash)
                    .param("retentionSeconds", retention.toSeconds())
                    .update();
            IdempotentCommandResult result = command.get();
            jdbc.sql("""
                            UPDATE platform.idempotency_record
                            SET response_status = :status,
                                response_body = CAST(:responseBody AS jsonb)
                            WHERE organization_id = :organizationId
                              AND actor_subject = :actorSubject
                              AND idempotency_key = :idempotencyKey
                            """)
                    .param("status", result.status())
                    .param("responseBody", result.responseBody())
                    .param("organizationId", scope.organizationId())
                    .param("actorSubject", scope.actor().subject())
                    .param("idempotencyKey", idempotencyKey)
                    .update();
            return new IdempotentCommandResult(result.status(), result.responseBody(), false);
        });
    }

    private void validateKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,200}")) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must be 8-200 safe ASCII characters");
        }
    }

    static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record ExistingRecord(String requestHash, int status, String body) {
    }
}
