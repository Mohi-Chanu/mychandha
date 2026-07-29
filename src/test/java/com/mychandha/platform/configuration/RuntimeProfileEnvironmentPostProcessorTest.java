package com.mychandha.platform.configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class RuntimeProfileEnvironmentPostProcessorTest {

    @TempDir
    Path temporaryDirectory;

    private final RuntimeProfileEnvironmentPostProcessor processor =
            new RuntimeProfileEnvironmentPostProcessor();

    @Test
    void rejectsMissingRuntimeProfile() {
        assertThatThrownBy(() -> validate(new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Select exactly one runtime profile");
    }

    @Test
    void rejectsConflictingRuntimeProfiles() {
        MockEnvironment environment = configured("api", "dispatcher");

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mutually exclusive");
    }

    @Test
    void rejectsLocalProductionCombination() {
        MockEnvironment environment = configured("local", "production");

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be combined");
    }

    @Test
    void rejectsLocalRuntimeCombination() {
        MockEnvironment environment = configured("local", "api");

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be combined with runtime");
    }

    @Test
    void rejectsFlywayInApiRuntime() {
        MockEnvironment environment = configured("api");
        environment.setProperty("spring.flyway.enabled", "true");

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Flyway must be disabled");
    }

    @Test
    void rejectsPlaceholderCredentialsOutsideLocalAndTest() {
        MockEnvironment environment = configured("dispatcher");
        environment.setProperty("spring.datasource.password", "change-me");

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void acceptsExactlyOneConfiguredRuntime() {
        assertThatCode(() -> validate(configured("migration")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsProductionWithoutVerifyFullAndRootCertificate() {
        MockEnvironment environment = configured("production", "migration");

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("verify-full");
    }

    @Test
    void rejectsProductionWithMissingRootCertificateFile() {
        MockEnvironment environment = configured("production", "migration");
        environment.setProperty(
                "spring.datasource.hikari.data-source-properties.sslmode",
                "verify-full");
        environment.setProperty(
                "spring.datasource.hikari.data-source-properties.sslrootcert",
                temporaryDirectory.resolve("missing.crt").toString());

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("absolute readable file");
    }

    @Test
    void rejectsProductionWithRelativeRootCertificatePath() {
        MockEnvironment environment = configured("production", "migration");
        environment.setProperty(
                "spring.datasource.hikari.data-source-properties.sslmode",
                "verify-full");
        environment.setProperty(
                "spring.datasource.hikari.data-source-properties.sslrootcert",
                "supabase-ca.crt");

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("absolute readable file");
    }

    @Test
    void rejectsProductionDatabaseUrlTlsOverrides() throws IOException {
        Path certificate = temporaryDirectory.resolve("supabase-ca.crt");
        Files.writeString(certificate, "test-ca");
        MockEnvironment environment = configured("production", "migration");
        environment.setProperty(
                "spring.datasource.url",
                "jdbc:postgresql://database:5432/mychandha?SSLMODE=require");
        environment.setProperty(
                "spring.datasource.hikari.data-source-properties.sslmode",
                "verify-full");
        environment.setProperty(
                "spring.datasource.hikari.data-source-properties.sslrootcert",
                certificate.toAbsolutePath().toString());

        assertThatThrownBy(() -> validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not override");
    }

    @Test
    void acceptsProductionWithVerifyFullAndReadableRootCertificate()
            throws IOException {
        Path certificate = temporaryDirectory.resolve("supabase-ca.crt");
        Files.writeString(certificate, "test-ca");
        MockEnvironment environment = configured("production", "migration");
        environment.setProperty(
                "spring.datasource.hikari.data-source-properties.sslmode",
                "verify-full");
        environment.setProperty(
                "spring.datasource.hikari.data-source-properties.sslrootcert",
                certificate.toAbsolutePath().toString());

        assertThatCode(() -> validate(environment)).doesNotThrowAnyException();
    }

    @Test
    void acceptsGuardedLocalMode() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertThatCode(() -> validate(environment))
                .doesNotThrowAnyException();
    }

    private MockEnvironment configured(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        environment.setProperty(
                "spring.datasource.url",
                "jdbc:postgresql://database:5432/mychandha");
        environment.setProperty("spring.datasource.username", "runtime_role");
        environment.setProperty("spring.datasource.password", "runtime-password");
        environment.setProperty(
                "mychandha.identity.issuer",
                "https://test.supabase.co/auth/v1");
        environment.setProperty(
                "mychandha.identity.jwk-set-uri",
                "https://test.supabase.co/auth/v1/.well-known/jwks.json");
        environment.setProperty("spring.flyway.enabled", "false");
        return environment;
    }

    private void validate(MockEnvironment environment) {
        processor.postProcessEnvironment(environment, new SpringApplication());
    }
}
