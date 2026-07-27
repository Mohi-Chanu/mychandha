package com.mychandha.platform.security.ratelimit;

enum RateLimitEndpointClass {
    HEALTH("health"),
    METRICS("metrics"),
    API_READ("api_read"),
    API_COMMAND("api_command"),
    OTHER("other");

    private final String tag;

    RateLimitEndpointClass(String tag) {
        this.tag = tag;
    }

    String tag() {
        return tag;
    }
}
