package com.mychandha.platform.events;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mychandha.outbox")
public record OutboxProperties(
        @NotNull Duration pollDelay,
        @NotNull Duration lockTimeout,
        @Min(1) @Max(500) int batchSize,
        @Min(1) @Max(100) int maxAttempts) {
}
