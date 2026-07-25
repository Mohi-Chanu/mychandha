package com.mychandha.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mychandha.platform.MyChandhaApplication;
import com.mychandha.platform.events.OutboxPublisher;
import com.mychandha.platform.identity.PlatformIdentityController;
import com.mychandha.platform.runtime.MigrationExitRunner;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

class PlatformMigrationIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("mychandha")
            .withUsername("mychandha_app")
            .withPassword("test-password");

    @BeforeAll
    static void migrate() throws Exception {
        POSTGRES.start();
        try (Connection connection = ownerConnection()) {
            connection.createStatement().execute("""
                    CREATE ROLE mychandha_api
                        NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
                        NOREPLICATION NOBYPASSRLS
                    """);
            connection.createStatement().execute("""
                    CREATE ROLE mychandha_dispatcher
                        NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
                        NOREPLICATION NOBYPASSRLS
                    """);
        }
        org.flywaydb.core.Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        try (Connection connection = ownerConnection()) {
            connection.createStatement().execute(
                    "CREATE ROLE api_runtime LOGIN PASSWORD 'runtime-password'");
            connection.createStatement().execute(
                    "GRANT mychandha_api TO api_runtime");
            connection.createStatement().execute(
                    "CREATE ROLE dispatcher_runtime LOGIN PASSWORD 'dispatcher-password'");
            connection.createStatement().execute(
                    "GRANT mychandha_dispatcher TO dispatcher_runtime");
        }
    }

    @AfterAll
    static void stop() {
        POSTGRES.stop();
    }

    @Test
    void migrationAppliesRuntimeRoleIsolation() throws Exception {
        try (Connection connection = ownerConnection()) {
            assertThat(count(connection, """
                    SELECT count(*) FROM flyway_schema_history
                    WHERE version IN ('1', '2') AND success
                    """)).isEqualTo(2);
            try (var result = connection.createStatement().executeQuery("""
                    SELECT rolsuper, rolcreatedb, rolcreaterole, rolreplication,
                           rolbypassrls
                    FROM pg_roles
                    WHERE rolname = 'mychandha_api'
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBoolean("rolsuper")).isFalse();
                assertThat(result.getBoolean("rolcreatedb")).isFalse();
                assertThat(result.getBoolean("rolcreaterole")).isFalse();
                assertThat(result.getBoolean("rolreplication")).isFalse();
                assertThat(result.getBoolean("rolbypassrls")).isFalse();
            }
            assertThat(booleanResult(connection, """
                    SELECT has_function_privilege(
                        'dispatcher_runtime',
                        'platform.claim_outbox_events(text,integer,bigint)',
                        'EXECUTE'
                    )
                    """)).isTrue();
            assertThat(booleanResult(connection, """
                    SELECT has_function_privilege(
                        'api_runtime',
                        'platform.claim_outbox_events(text,integer,bigint)',
                        'EXECUTE'
                    )
                    """)).isFalse();
            assertThat(booleanResult(connection, """
                    SELECT has_table_privilege(
                        'dispatcher_runtime',
                        'platform.outbox_event',
                        'SELECT'
                    )
                    """)).isFalse();
        }
    }

    @Test
    void rowLevelSecuritySeparatesOrganizations() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        insertOrganization(first, "First");
        insertOrganization(second, "Second");

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            setOrganization(connection, first);
            assertThat(count(connection,
                    "SELECT count(*) FROM organization.organization WHERE id = '" + first + "'"))
                    .isEqualTo(1);
            assertThat(count(connection,
                    "SELECT count(*) FROM organization.organization WHERE id = '" + second + "'"))
                    .isZero();
            connection.rollback();
        }
    }

    @Test
    void auditEventsCannotBeChanged() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        insertOrganization(organizationId, "Audit Org");
        try (Connection connection = ownerConnection()) {
            connection.setAutoCommit(false);
            setOrganization(connection, organizationId);
            connection.createStatement().executeUpdate("""
                    INSERT INTO audit.audit_event (
                        id, organization_id, sequence_number, event_type, actor_provider, actor_subject,
                        resource_type, event_data, event_data_canonical, event_hash
                    ) VALUES (
                        '%s', '%s', 1, 'TEST', 'supabase', 'subject',
                        'test', '{}'::jsonb, '{}', 'hash'
                    )
                    """.formatted(eventId, organizationId));
            connection.commit();
        }
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            setOrganization(connection, organizationId);
            assertThatThrownBy(() -> connection.createStatement().executeUpdate(
                    "DELETE FROM audit.audit_event WHERE id = '" + eventId + "'"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("immutable");
            connection.rollback();
        }
    }

    @Test
    void roleAssignmentsCannotCrossOrganizationBoundaries() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID secondOrganizationRoleId = UUID.randomUUID();
        insertOrganization(first, "First Role Org");
        insertOrganization(second, "Second Role Org");

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            connection.createStatement().executeUpdate("""
                    INSERT INTO identity.user_profile (id) VALUES ('%s')
                    """.formatted(userId));
            setOrganization(connection, first);
            connection.createStatement().executeUpdate("""
                    INSERT INTO identity.membership (
                        id, organization_id, user_id, status
                    ) VALUES ('%s', '%s', '%s', 'ACTIVE')
                    """.formatted(membershipId, first, userId));
            connection.commit();
        }
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            setOrganization(connection, second);
            connection.createStatement().executeUpdate("""
                    INSERT INTO identity.role (
                        id, organization_id, code, display_name
                    ) VALUES ('%s', '%s', 'TREASURER', 'Treasurer')
                    """.formatted(secondOrganizationRoleId, second));
            connection.commit();
        }
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            setOrganization(connection, first);
            assertThatThrownBy(() -> connection.createStatement().executeUpdate("""
                    INSERT INTO identity.membership_role (
                        organization_id, membership_id, role_id
                    ) VALUES ('%s', '%s', '%s')
                    """.formatted(first, membershipId, secondOrganizationRoleId)))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("membership_role_organization_id_role_id_fkey");
            connection.rollback();
        }
    }

    @Test
    void inboxEventIdentityIsScopedToOrganization() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        insertOrganization(first, "First Inbox Org");
        insertOrganization(second, "Second Inbox Org");

        insertInboxEvent(first, "razorpay", "event-shared");
        insertInboxEvent(second, "razorpay", "event-shared");

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            setOrganization(connection, first);
            assertThat(count(connection, """
                    SELECT count(*) FROM platform.inbox_event
                    WHERE source = 'razorpay' AND external_event_id = 'event-shared'
                    """)).isEqualTo(1);
            connection.rollback();
        }
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            setOrganization(connection, second);
            assertThat(count(connection, """
                    SELECT count(*) FROM platform.inbox_event
                    WHERE source = 'razorpay' AND external_event_id = 'event-shared'
                    """)).isEqualTo(1);
            connection.rollback();
        }
    }

    @Test
    void apiRoleCannotUseDdlOrDispatcherFunctions() throws Exception {
        try (Connection connection = connection()) {
            assertThatThrownBy(() -> connection.createStatement().execute(
                    "CREATE TABLE platform.forbidden_table (id UUID)"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("permission denied");
            assertThatThrownBy(() -> connection.createStatement().executeQuery(
                    "SELECT * FROM platform.claim_outbox_events('api', 1, 120)"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("permission denied");
        }
    }

    @Test
    void apiRoleCanEnqueueOnlyInsideItsTenantAndCannotReadOutbox() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        insertOrganization(organizationId, "API Outbox");

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            setOrganization(connection, organizationId);
            connection.createStatement().executeUpdate("""
                    INSERT INTO platform.outbox_event (
                        id, organization_id, aggregate_type, aggregate_id,
                        event_type, schema_version, payload
                    ) VALUES (
                        '%s', '%s', 'organization', '%s',
                        'ApiEnqueued', 1, '{}'::jsonb
                    )
                    """.formatted(eventId, organizationId, organizationId));
            connection.commit();

            assertThatThrownBy(() -> connection.createStatement().executeQuery(
                    "SELECT * FROM platform.outbox_event"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("permission denied");
        }

        try (Connection owner = ownerConnection()) {
            owner.createStatement().executeUpdate("""
                    UPDATE platform.outbox_event
                    SET status = 'PUBLISHED', published_at = now()
                    WHERE id = '%s'
                    """.formatted(eventId));
        }
    }

    @Test
    void dispatcherUsesOnlyControlledOutboxRoutines() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        insertOrganization(organizationId, "Dispatcher Boundary");
        try (Connection connection = ownerConnection()) {
            connection.createStatement().executeUpdate("""
                    INSERT INTO platform.outbox_event (
                        id, organization_id, aggregate_type, aggregate_id,
                        event_type, schema_version, payload
                    ) VALUES (
                        '%s', '%s', 'organization', '%s',
                        'BoundaryChecked', 1, '{}'::jsonb
                    )
                    """.formatted(eventId, organizationId, organizationId));
        }

        try (Connection connection = dispatcherConnection()) {
            assertThatThrownBy(() -> connection.createStatement().executeQuery(
                    "SELECT * FROM platform.outbox_event"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("permission denied");

            try (var claimed = connection.createStatement().executeQuery(
                    "SELECT * FROM platform.claim_outbox_events('dispatcher-one', 1, 120)")) {
                assertThat(claimed.next()).isTrue();
                assertThat(claimed.getObject("id", UUID.class)).isEqualTo(eventId);
                assertThat(claimed.getBoolean("reclaimed")).isFalse();
                assertThat(claimed.getString("payload")).isEqualTo("{}");
            }

            assertThat(booleanResult(connection, """
                    SELECT platform.mark_outbox_published(
                        '%s', 'dispatcher-two'
                    )
                    """.formatted(eventId))).isFalse();
            assertThat(booleanResult(connection, """
                    SELECT platform.mark_outbox_published(
                        '%s', 'dispatcher-one'
                    )
                    """.formatted(eventId))).isTrue();
        }
    }

    @Test
    void dispatcherRoutinesValidateLimitsAndExposeAggregateHealthOnly() throws Exception {
        try (Connection connection = dispatcherConnection()) {
            assertThatThrownBy(() -> connection.createStatement().executeQuery(
                    "SELECT * FROM platform.claim_outbox_events('dispatcher', 0, 120)"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("batch size");

            try (var result = connection.createStatement().executeQuery(
                    "SELECT * FROM platform.outbox_backlog(120)")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getMetaData().getColumnCount()).isEqualTo(5);
                assertThat(result.getMetaData().getColumnLabel(1)).isEqualTo("pending");
                assertThat(result.getMetaData().getColumnLabel(5))
                        .isEqualTo("stale_processing");
            }
        }
    }

    @Test
    void staleClaimCanBeReclaimedAndDeadLetteredByItsOwner() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        insertOrganization(organizationId, "Dispatcher Recovery");
        try (Connection connection = ownerConnection()) {
            connection.createStatement().executeUpdate("""
                    INSERT INTO platform.outbox_event (
                        id, organization_id, aggregate_type, aggregate_id,
                        event_type, schema_version, payload, status, attempts,
                        locked_at, locked_by
                    ) VALUES (
                        '%s', '%s', 'organization', '%s',
                        'RecoveryChecked', 1, '{}'::jsonb, 'PROCESSING', 11,
                        now() - interval '10 minutes', 'terminated'
                    )
                    """.formatted(eventId, organizationId, organizationId));
        }

        try (Connection connection = dispatcherConnection();
             var claimed = connection.createStatement().executeQuery(
                     "SELECT * FROM platform.claim_outbox_events('replacement', 1, 120)")) {
            assertThat(claimed.next()).isTrue();
            assertThat(claimed.getObject("id", UUID.class)).isEqualTo(eventId);
            assertThat(claimed.getBoolean("reclaimed")).isTrue();
            assertThat(claimed.getInt("attempts")).isEqualTo(12);
        }

        try (Connection connection = dispatcherConnection()) {
            assertThat(booleanResult(connection, """
                    SELECT platform.reschedule_outbox_event(
                        '%s', 'replacement', 1, 'DeliveryFailed', true
                    )
                    """.formatted(eventId))).isTrue();
        }

        try (Connection connection = ownerConnection();
             var result = connection.createStatement().executeQuery("""
                     SELECT status, last_error_code
                     FROM platform.outbox_event
                     WHERE id = '%s'
                     """.formatted(eventId))) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("status")).isEqualTo("DEAD_LETTER");
            assertThat(result.getString("last_error_code")).isEqualTo("DeliveryFailed");
        }
    }

    @Test
    void runtimeProfilesStartWithSeparatedResponsibilities() throws Exception {
        try (ConfigurableApplicationContext api = startRuntime("api", Map.of(
                "API_DATABASE_URL", POSTGRES.getJdbcUrl(),
                "API_DATABASE_USERNAME", "api_runtime",
                "API_DATABASE_PASSWORD", "runtime-password",
                "SUPABASE_JWT_ISSUER", "https://test.supabase.co/auth/v1",
                "SUPABASE_JWKS_URI",
                "https://test.supabase.co/auth/v1/.well-known/jwks.json",
                "server.port", "0"))) {
            assertThat(api.containsBean("flyway")).isFalse();
            assertThat(api.getBeansOfType(PlatformIdentityController.class)).hasSize(1);
            assertThat(api.getBeansOfType(OutboxPublisher.class)).isEmpty();
        }

        try (Connection owner = ownerConnection()) {
            owner.createStatement().executeUpdate("""
                    UPDATE platform.outbox_event
                    SET status = 'PUBLISHED', published_at = now(),
                        locked_at = NULL, locked_by = NULL
                    WHERE status IN ('PENDING', 'PROCESSING')
                    """);
        }

        try (ConfigurableApplicationContext dispatcher = startRuntime(
                "dispatcher",
                Map.of(
                        "DISPATCHER_DATABASE_URL", POSTGRES.getJdbcUrl(),
                        "DISPATCHER_DATABASE_USERNAME", "dispatcher_runtime",
                        "DISPATCHER_DATABASE_PASSWORD", "dispatcher-password",
                        "mychandha.outbox.poll-delay", "PT1H"))) {
            assertThat(dispatcher.containsBean("flyway")).isFalse();
            assertThat(dispatcher.getBeansOfType(PlatformIdentityController.class)).isEmpty();
            assertThat(dispatcher.getBeansOfType(OutboxPublisher.class)).hasSize(1);
        }
    }

    @Test
    void migrationProfileRunsFlywayAndCompletesAsOneOffRuntime() {
        try (ConfigurableApplicationContext migration = startRuntime(
                "migration",
                Map.of(
                        "MIGRATION_DATABASE_URL", POSTGRES.getJdbcUrl(),
                        "MIGRATION_DATABASE_USERNAME", POSTGRES.getUsername(),
                        "MIGRATION_DATABASE_PASSWORD", POSTGRES.getPassword()))) {
            assertThat(migration.containsBean("flyway")).isTrue();
            MigrationExitRunner exitRunner = migration.getBean(MigrationExitRunner.class);
            exitRunner.closeAfterSuccessfulStartup(migration);
            assertThat(migration.isActive()).isFalse();
        }
    }

    private void insertInboxEvent(
            UUID organizationId,
            String source,
            String externalEventId) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            setOrganization(connection, organizationId);
            try (var statement = connection.prepareStatement("""
                    INSERT INTO platform.inbox_event (
                        organization_id, source, external_event_id, payload_hash
                    ) VALUES (?, ?, ?, 'payload-hash')
                    """)) {
                statement.setObject(1, organizationId);
                statement.setString(2, source);
                statement.setString(3, externalEventId);
                statement.executeUpdate();
            }
            connection.commit();
        }
    }

    private void insertOrganization(UUID organizationId, String name) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            setOrganization(connection, organizationId);
            connection.createStatement().executeUpdate("""
                    INSERT INTO organization.organization (id, legal_name, display_name, status)
                    VALUES ('%s', '%s', '%s', 'ACTIVE')
                    """.formatted(organizationId, name, name));
            connection.commit();
        }
    }

    private static void setOrganization(Connection connection, UUID organizationId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT set_config('app.current_organization_id', ?, true)")) {
            statement.setString(1, organizationId.toString());
            statement.execute();
        }
    }

    private static long count(Connection connection, String sql) throws SQLException {
        try (var resultSet = connection.createStatement().executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), "api_runtime", "runtime-password");
    }

    private static Connection dispatcherConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), "dispatcher_runtime", "dispatcher-password");
    }

    private static Connection ownerConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static boolean booleanResult(Connection connection, String sql)
            throws SQLException {
        try (var result = connection.createStatement().executeQuery(sql)) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private static ConfigurableApplicationContext startRuntime(
            String profile,
            Map<String, Object> properties) {
        Map<String, Object> defaults = new HashMap<>(properties);
        defaults.put("spring.main.banner-mode", "off");
        defaults.put("spring.main.log-startup-info", "false");
        SpringApplication application = new SpringApplication(MyChandhaApplication.class);
        application.setAdditionalProfiles(profile);
        application.setDefaultProperties(defaults);
        return application.run();
    }
}
