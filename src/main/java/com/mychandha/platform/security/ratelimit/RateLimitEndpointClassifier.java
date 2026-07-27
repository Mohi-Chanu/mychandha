package com.mychandha.platform.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
@Profile({"api", "local", "test"})
public final class RateLimitEndpointClassifier {

    RateLimitEndpointClass classify(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator/health")) {
            return RateLimitEndpointClass.HEALTH;
        }
        if (path.equals("/actuator/prometheus")) {
            return RateLimitEndpointClass.METRICS;
        }
        if (path.startsWith("/api/")) {
            String method = request.getMethod();
            return HttpMethod.GET.matches(method)
                            || HttpMethod.HEAD.matches(method)
                            || HttpMethod.OPTIONS.matches(method)
                    ? RateLimitEndpointClass.API_READ
                    : RateLimitEndpointClass.API_COMMAND;
        }
        return RateLimitEndpointClass.OTHER;
    }
}
