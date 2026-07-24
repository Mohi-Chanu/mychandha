package com.mychandha.platform.configuration;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Render exposes PostgreSQL as a postgres:// URI while JDBC requires a
 * jdbc:postgresql:// URL. This adapter keeps application configuration portable
 * across Render and any environment that already supplies a JDBC URL.
 */
public final class DatabaseUrlEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {
        String raw = environment.getProperty("DATABASE_URL");
        if (raw == null || !(raw.startsWith("postgres://") || raw.startsWith("postgresql://"))) {
            return;
        }
        URI uri = URI.create(raw);
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        Map<String, Object> normalized = new HashMap<>();
        normalized.put("spring.datasource.url",
                "jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath()
                        + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()));
        if (uri.getUserInfo() != null) {
            String[] userInfo = uri.getRawUserInfo().split(":", 2);
            normalized.putIfAbsent("spring.datasource.username", decode(userInfo[0]));
            if (userInfo.length == 2) {
                normalized.putIfAbsent("spring.datasource.password", decode(userInfo[1]));
            }
        }
        environment.getPropertySources().addFirst(
                new MapPropertySource("normalizedDatabaseUrl", normalized));
    }

    private String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
