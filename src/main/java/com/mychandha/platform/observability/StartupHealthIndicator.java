package com.mychandha.platform.observability;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("startupState")
@Profile({"api", "dispatcher", "local"})
public final class StartupHealthIndicator
        implements HealthIndicator, ApplicationListener<ApplicationReadyEvent> {

    private final AtomicBoolean ready = new AtomicBoolean();

    @Override
    public Health health() {
        return ready.get()
                ? Health.up().build()
                : Health.outOfService().build();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ready.set(true);
    }
}
