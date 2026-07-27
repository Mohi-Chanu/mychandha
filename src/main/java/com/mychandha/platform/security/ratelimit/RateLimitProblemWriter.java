package com.mychandha.platform.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mychandha.platform.security.ApiProblemDetails;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
@Profile({"api", "local", "test"})
final class RateLimitProblemWriter {

    private final ObjectMapper objectMapper;

    RateLimitProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void write(HttpServletResponse response, RateLimitDecision decision) throws IOException {
        ProblemDetail problem = ApiProblemDetails.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests",
                "RATE_LIMIT_EXCEEDED",
                "Request rate exceeded. Retry after the indicated delay.");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
