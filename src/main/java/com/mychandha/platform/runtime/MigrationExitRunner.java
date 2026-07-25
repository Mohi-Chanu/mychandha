package com.mychandha.platform.runtime;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Closes a successful migration-only application after Flyway and application
 * initialization have completed. Startup failures propagate as non-zero
 * process exits from Spring Boot.
 */
@Component
@Profile("migration")
public final class MigrationExitRunner implements ApplicationRunner {

    private final AtomicBoolean completed = new AtomicBoolean();

    @Override
    public void run(ApplicationArguments arguments) {
        completed.set(true);
    }

    public void closeAfterSuccessfulStartup(ConfigurableApplicationContext context) {
        if (!completed.get()) {
            throw new IllegalStateException("Migration runtime did not complete startup");
        }
        context.close();
    }
}
