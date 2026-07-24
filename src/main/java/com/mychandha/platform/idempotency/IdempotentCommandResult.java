package com.mychandha.platform.idempotency;

public record IdempotentCommandResult(int status, String responseBody, boolean replayed) {
}
