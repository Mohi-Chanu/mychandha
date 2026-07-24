package com.mychandha.platform.idempotency;

public final class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("The Idempotency-Key was already used with a different request");
    }
}
