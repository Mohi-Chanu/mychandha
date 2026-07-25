package com.mychandha.platform.security;

import java.net.URI;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ApiProblemDetails {

    private static final String PROBLEM_BASE = "https://api.mychandha.in/problems/";

    private ApiProblemDetails() {
    }

    public static ProblemDetail create(
            HttpStatus status,
            String title,
            String code,
            String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(PROBLEM_BASE
                + code.toLowerCase(java.util.Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", MDC.get("correlationId"));
        return problem;
    }
}
