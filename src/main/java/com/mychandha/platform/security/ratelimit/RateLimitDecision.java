package com.mychandha.platform.security.ratelimit;

record RateLimitDecision(
        boolean allowed,
        long retryAfterSeconds,
        RateLimitScope scope,
        RateLimitEndpointClass endpointClass,
        String outcome) {

    static RateLimitDecision allowed(
            RateLimitScope scope,
            RateLimitEndpointClass endpointClass) {
        return new RateLimitDecision(true, 0, scope, endpointClass, "allowed");
    }

    static RateLimitDecision rejected(
            long retryAfterSeconds,
            RateLimitScope scope,
            RateLimitEndpointClass endpointClass,
            String outcome) {
        return new RateLimitDecision(
                false,
                Math.max(1, retryAfterSeconds),
                scope,
                endpointClass,
                outcome);
    }
}
