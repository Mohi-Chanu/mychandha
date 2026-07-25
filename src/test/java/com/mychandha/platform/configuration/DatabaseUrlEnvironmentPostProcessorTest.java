package com.mychandha.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsRenderPostgresUriToJdbcProperties() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of("DATABASE_URL",
                        "postgres://app%40user:s%23cret%2Bvalue@db.internal:5432/mychandha?sslmode=require")));

        new DatabaseUrlEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.internal:5432/mychandha?sslmode=require");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("app@user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("s#cret+value");
    }

    @Test
    void usesCredentialClassForSelectedRuntime() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("api");
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of(
                        "DATABASE_URL", "postgres://wrong:wrong@legacy:5432/legacy",
                        "API_DATABASE_URL",
                        "postgres://api_user:api_password@db.internal:5432/mychandha")));

        new DatabaseUrlEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.internal:5432/mychandha");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("api_user");
    }
}
