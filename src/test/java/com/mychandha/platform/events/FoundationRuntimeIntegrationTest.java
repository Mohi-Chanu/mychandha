package com.mychandha.platform.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychandha.platform.audit.AuditEvent;
import com.mychandha.platform.audit.AuditIntegrityService;
import com.mychandha.platform.audit.AuditService;
import com.mychandha.platform.idempotency.IdempotencyService;
import com.mychandha.platform.idempotency.IdempotentCommandResult;
import com.mychandha.platform.identity.ExternalIdentity;
import com.mychandha.platform.tenancy.OrganizationContext;
import com.mychandha.platform.tenancy.TenantJdbcExecutor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

class FoundationRuntimeIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("mychandha")
                    .withUsername("mychandha_owner")
                    .withPassword("owner-password");

    @BeforeAll
    static void migrate() throws Exception {
        POSTGRES.start();
        org.flywaydb.core.Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        try (Connection connection = ownerConnection()) {
            connection.createStatement().execute(
                    "CREATE ROLE app_runtime LOGIN PASSWORD 'runtime-password'");
            connection.createStatement().execute(
                    "GRANT USAGE ON SCHEMA organization, identity, audit, platform TO app_runtime");
            connection.createStatement().execute(
                    "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA "
                            + "organization, identity, audit, platform TO app_runtime");
            connection.createStatement().execute(
                    "GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA platform TO app_runtime");
        }
    }

    @AfterAll
    static void stop() {
        POSTGRES.stop();
    }

    @Test
    void staleOutboxClaimIsRecovered() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        insertOrganization(organizationId, "Outbox Recovery");
        try (Connection connection = ownerConnection()) {
            connection.createStatement().executeUpdate("""
                    INSERT INTO platform.outbox_event (
                        id, organization_id, aggregate_type, aggregate_id,
                        event_type, schema_version, payload, status, attempts,
                        locked_at, locked_by
                    ) VALUES (
                        '%s', '%s', 'organization', '%s',
                        'RecoveryRequired', 1, '{}'::jsonb, 'PROCESSING', 1,
                        now() - interval '10 minutes', 'terminated-worker'
                    )
                    """.formatted(eventId, organizationId, organizationId));
        }

        DriverManagerDataSource ownerDataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var transactionManager = new DataSourceTransactionManager(ownerDataSource);
        SimpleMeterRegistry metrics = new SimpleMeterRegistry();
        try {
            OutboxPublisher publisher = new OutboxPublisher(
                    JdbcClient.create(ownerDataSource),
                    new TransactionTemplate(transactionManager),
                    event -> {
                    },
                    new OutboxProperties(
                            Duration.ofSeconds(1),
                            Duration.ofMinutes(2),
                            50,
                            12),
                    metrics);

            var claimed = publisher.claim();

            assertThat(claimed).hasSize(1);
            assertThat(claimed.getFirst().id()).isEqualTo(eventId);
            assertThat(claimed.getFirst().attempts()).isEqualTo(2);
        } finally {
            metrics.close();
        }
    }

    @Test
    void concurrentIdempotentRequestsExecuteCommandOnce() throws Exception {
        UUID organizationId = UUID.randomUUID();
        insertOrganization(organizationId, "Idempotency");
        IdempotencyService service = idempotencyService();
        ExternalIdentity actor = new ExternalIdentity(
                "supabase", UUID.randomUUID().toString(), null, null, Map.of());
        AtomicInteger commandExecutions = new AtomicInteger();
        CountDownLatch firstCommandStarted = new CountDownLatch(1);
        CountDownLatch allowFirstCommandToFinish = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> withOrganization(organizationId, actor, () ->
                    service.execute(
                            "same-request-key",
                            "{}".getBytes(StandardCharsets.UTF_8),
                            Duration.ofMinutes(10),
                            () -> {
                                commandExecutions.incrementAndGet();
                                firstCommandStarted.countDown();
                                await(allowFirstCommandToFinish);
                                return new IdempotentCommandResult(201, "{\"id\":\"one\"}", false);
                            })));
            assertThat(firstCommandStarted.await(10, TimeUnit.SECONDS)).isTrue();

            var second = executor.submit(() -> withOrganization(organizationId, actor, () ->
                    service.execute(
                            "same-request-key",
                            "{}".getBytes(StandardCharsets.UTF_8),
                            Duration.ofMinutes(10),
                            () -> {
                                commandExecutions.incrementAndGet();
                                return new IdempotentCommandResult(201, "{\"id\":\"two\"}", false);
                            })));
            allowFirstCommandToFinish.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS).replayed()).isFalse();
            assertThat(second.get(10, TimeUnit.SECONDS).replayed()).isTrue();
            assertThat(commandExecutions).hasValue(1);
        }
    }

    @Test
    void auditHashChainCanBeRecomputedFromStoredEvidence() throws Exception {
        UUID organizationId = UUID.randomUUID();
        insertOrganization(organizationId, "Audit Integrity");
        ExternalIdentity actor = new ExternalIdentity(
                "supabase", UUID.randomUUID().toString(), null, null, Map.of());
        TenantJdbcExecutor tenantJdbc = runtimeTenantExecutor();
        AuditService audit = new AuditService(tenantJdbc, new ObjectMapper());
        AuditIntegrityService integrity = new AuditIntegrityService(tenantJdbc);

        var result = withOrganization(organizationId, actor, () -> {
            audit.append(new AuditEvent(
                    "ORGANIZATION_ACCESSED",
                    "organization",
                    organizationId.toString(),
                    Map.of("reason", "validation")));
            audit.append(new AuditEvent(
                    "PERMISSION_CHECKED",
                    "organization",
                    organizationId.toString(),
                    Map.of("permission", "platform.access")));
            return integrity.verifyCurrentOrganization();
        });

        assertThat(result.valid()).isTrue();
        assertThat(result.eventCount()).isEqualTo(2);
        assertThat(result.lastHash()).hasSize(64);
    }

    private static IdempotencyService idempotencyService() {
        return new IdempotencyService(runtimeTenantExecutor());
    }

    private static TenantJdbcExecutor runtimeTenantExecutor() {
        DriverManagerDataSource runtimeDataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), "app_runtime", "runtime-password");
        JdbcClient jdbc = JdbcClient.create(runtimeDataSource);
        var transactionManager = new DataSourceTransactionManager(runtimeDataSource);
        return new TenantJdbcExecutor(jdbc, transactionManager);
    }

    private static <T> T withOrganization(
            UUID organizationId,
            ExternalIdentity actor,
            java.util.concurrent.Callable<T> work) throws Exception {
        OrganizationContext.set(organizationId, actor);
        try {
            return work.call();
        } finally {
            OrganizationContext.clear();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent request");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent request", exception);
        }
    }

    private static void insertOrganization(UUID organizationId, String name) throws Exception {
        try (Connection connection = ownerConnection()) {
            connection.setAutoCommit(false);
            try (var context = connection.prepareStatement(
                    "SELECT set_config('app.current_organization_id', ?, true)")) {
                context.setString(1, organizationId.toString());
                context.execute();
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO organization.organization (
                        id, legal_name, display_name, status
                    ) VALUES (?, ?, ?, 'ACTIVE')
                    """)) {
                statement.setObject(1, organizationId);
                statement.setString(2, name);
                statement.setString(3, name);
                statement.executeUpdate();
            }
            connection.commit();
        }
    }

    private static Connection ownerConnection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
