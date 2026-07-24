package com.mychandha.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class PlatformMigrationIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("mychandha")
            .withUsername("mychandha_app")
            .withPassword("test-password");

    @BeforeAll
    static void migrate() throws Exception {
        POSTGRES.start();
        org.flywaydb.core.Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            connection.createStatement().execute("CREATE ROLE app_runtime LOGIN PASSWORD 'runtime-password'");
            connection.createStatement().execute(
                    "GRANT USAGE ON SCHEMA organization, identity, audit, platform TO app_runtime");
            connection.createStatement().execute(
                    "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA organization, identity, audit, platform TO app_runtime");
            connection.createStatement().execute(
                    "GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA platform TO app_runtime");
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
        try (Connection connection = connection()) {
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
                POSTGRES.getJdbcUrl(), "app_runtime", "runtime-password");
    }
}
