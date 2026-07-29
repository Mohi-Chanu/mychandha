package com.mychandha.platform.configuration;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Enforces runtime separation before the application can create a datasource
 * or run Flyway.
 */
public final class RuntimeProfileEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    private static final Set<String> RUNTIME_PROFILES =
            Set.of("api", "dispatcher", "migration");
    private static final Set<String> SAFE_NON_RUNTIME_PROFILES =
            Set.of("local", "test");

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {
        Set<String> profiles = Arrays.stream(environment.getActiveProfiles())
                .collect(Collectors.toUnmodifiableSet());
        long runtimeCount = profiles.stream().filter(RUNTIME_PROFILES::contains).count();
        long safeNonRuntimeCount =
                profiles.stream().filter(SAFE_NON_RUNTIME_PROFILES::contains).count();
        boolean safeNonRuntime = profiles.stream().anyMatch(SAFE_NON_RUNTIME_PROFILES::contains);

        if (runtimeCount == 0 && !safeNonRuntime) {
            throw new IllegalStateException(
                    "Select exactly one runtime profile: api, dispatcher, or migration");
        }
        if (runtimeCount > 1) {
            throw new IllegalStateException(
                    "Runtime profiles api, dispatcher, and migration are mutually exclusive");
        }
        if (safeNonRuntimeCount > 1 || safeNonRuntime && runtimeCount > 0) {
            throw new IllegalStateException(
                    "Local and test modes cannot be combined with runtime profiles");
        }
        if (profiles.contains("production") && safeNonRuntime) {
            throw new IllegalStateException(
                    "Local or test mode cannot be combined with the production profile");
        }
        if (profiles.contains("production") && runtimeCount != 1) {
            throw new IllegalStateException(
                    "Production requires exactly one api, dispatcher, or migration profile");
        }

        String runtimeProfile = profiles.stream()
                .filter(RUNTIME_PROFILES::contains)
                .findFirst()
                .orElse(safeNonRuntime ? profiles.contains("test") ? "test" : "local" : "");
        if (("api".equals(runtimeProfile) || "dispatcher".equals(runtimeProfile))
                && environment.getProperty("spring.flyway.enabled", Boolean.class, false)) {
            throw new IllegalStateException(
                    "Flyway must be disabled for API and dispatcher runtimes");
        }
        if (!safeNonRuntime) {
            rejectPlaceholder(environment, "spring.datasource.url");
            rejectPlaceholder(environment, "spring.datasource.username");
            rejectPlaceholder(environment, "spring.datasource.password");
            if ("api".equals(runtimeProfile)) {
                rejectPlaceholder(environment, "mychandha.identity.issuer");
                rejectPlaceholder(environment, "mychandha.identity.jwk-set-uri");
            }
        }
        if (profiles.contains("production")) {
            validateDatabaseTls(environment);
        }
    }

    private void validateDatabaseTls(ConfigurableEnvironment environment) {
        String databaseUrl = environment.getProperty("spring.datasource.url", "");
        String normalizedUrl = databaseUrl.toLowerCase(Locale.ROOT);
        if (normalizedUrl.contains("sslmode=")
                || normalizedUrl.contains("sslrootcert=")
                || normalizedUrl.contains("sslfactory=")
                || normalizedUrl.contains("sslhostnameverifier=")
                || normalizedUrl.contains("ssl=")) {
            throw new IllegalStateException(
                    "Production database URL must not override the code-owned TLS policy");
        }
        String sslMode = environment.getProperty(
                "spring.datasource.hikari.data-source-properties.sslmode");
        if (!"verify-full".equals(sslMode)) {
            throw new IllegalStateException(
                    "Production database TLS mode must be verify-full");
        }
        String rootCertificate = environment.getProperty(
                "spring.datasource.hikari.data-source-properties.sslrootcert");
        if (rootCertificate == null || rootCertificate.isBlank()) {
            throw new IllegalStateException(
                    "Production database TLS root certificate is required");
        }
        try {
            Path certificatePath = Path.of(rootCertificate);
            if (!certificatePath.isAbsolute()
                    || !Files.isRegularFile(certificatePath)
                    || !Files.isReadable(certificatePath)) {
                throw new IllegalStateException(
                        "Production database TLS root certificate must be an absolute readable file");
            }
        } catch (InvalidPathException exception) {
            throw new IllegalStateException(
                    "Production database TLS root certificate path is invalid",
                    exception);
        }
    }

    private void rejectPlaceholder(ConfigurableEnvironment environment, String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank() || isPlaceholder(value)) {
            throw new IllegalStateException(
                    "Runtime database configuration is missing or contains a placeholder: "
                            + property);
        }
    }

    private boolean isPlaceholder(String value) {
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("change-me")
                || normalized.contains("placeholder")
                || normalized.contains("your-");
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
