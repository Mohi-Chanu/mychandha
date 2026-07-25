package com.mychandha.platform.observability;

import java.time.OffsetDateTime;

public record OutboxBacklog(
        long pending,
        OffsetDateTime oldest,
        long retryAttempts,
        long deadLetter,
        long staleProcessing) {
}
