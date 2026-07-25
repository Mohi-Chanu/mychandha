package com.mychandha.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

class StartupHealthIndicatorTest {

    @Test
    void startupIsDistinctFromLivenessAndChangesOnlyAfterReadyEvent() {
        StartupHealthIndicator indicator = new StartupHealthIndicator();

        assertThat(indicator.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);

        indicator.onApplicationEvent(null);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void startupContributorDoesNotClashWithHealthGroup() throws IOException {
        Component component = StartupHealthIndicator.class.getAnnotation(Component.class);

        assertThat(component.value()).isEqualTo("startupState");
        assertThat(component.value()).isNotEqualTo("startup");

        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (String profile : List.of("api", "dispatcher", "local")) {
            String resourceName = "application-" + profile + ".yml";
            var properties = loader.load(
                    resourceName, new ClassPathResource(resourceName)).getFirst();

            assertThat(properties.getProperty(
                    "management.endpoint.health.group.startup.include"))
                    .isEqualTo("startupState");
            assertThat(properties.getProperty(
                    "management.endpoint.health.group.readiness.include")
                    .toString())
                    .contains("startupState");
        }
    }
}
