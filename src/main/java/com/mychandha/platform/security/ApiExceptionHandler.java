package com.mychandha.platform.security;

import com.mychandha.platform.tenancy.OrganizationAccessDeniedException;
import com.mychandha.platform.tenancy.OrganizationContextMissingException;
import com.mychandha.platform.idempotency.IdempotencyConflictException;
import java.net.URI;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OrganizationContextMissingException.class)
    ProblemDetail missingOrganization(OrganizationContextMissingException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Organization context required",
                "ORGANIZATION_CONTEXT_REQUIRED", exception.getMessage());
    }

    @ExceptionHandler(OrganizationAccessDeniedException.class)
    ProblemDetail organizationDenied(OrganizationAccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Access denied",
                "ORGANIZATION_ACCESS_DENIED", "You do not have access to this organization.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidArgument(MethodArgumentNotValidException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request",
                "VALIDATION_FAILED", "One or more fields are invalid.");
        problem.setProperty("fieldErrors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> java.util.Map.of(
                        "field", error.getField(),
                        "message", java.util.Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value")))
                .toList());
        return problem;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail idempotencyConflict(IdempotencyConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Idempotency conflict",
                "IDEMPOTENCY_CONFLICT", exception.getMessage());
    }


    private ProblemDetail problem(HttpStatus status, String title, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://api.mychandha.in/problems/" + code.toLowerCase()
                .replace('_', '-')));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", MDC.get("correlationId"));
        return problem;
    }
}
