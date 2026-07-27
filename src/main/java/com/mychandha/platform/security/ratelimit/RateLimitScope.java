package com.mychandha.platform.security.ratelimit;

enum RateLimitScope {
    CLIENT_ADDRESS("client_address"),
    SUBJECT("subject"),
    ORGANIZATION("organization"),
    METRICS("metrics"),
    PROCESS("process");

    private final String tag;

    RateLimitScope(String tag) {
        this.tag = tag;
    }

    String tag() {
        return tag;
    }
}
