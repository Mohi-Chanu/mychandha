package com.mychandha.platform.security;

import com.mychandha.platform.idempotency.IdempotencyConflictException;
import com.mychandha.platform.tenancy.OrganizationAccessDeniedException;
import com.mychandha.platform.tenancy.OrganizationContextMissingException;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Profile({"api", "local", "test"})
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(OrganizationContextMissingException.class)
    ProblemDetail missingOrganization(OrganizationContextMissingException exception) {
        return ApiProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                "Organization context required",
                "ORGANIZATION_CONTEXT_REQUIRED",
                "A valid organization context is required.");
    }

    @ExceptionHandler(OrganizationAccessDeniedException.class)
    ProblemDetail organizationDenied(OrganizationAccessDeniedException exception) {
        return ApiProblemDetails.create(
                HttpStatus.FORBIDDEN,
                "Access denied",
                "ORGANIZATION_ACCESS_DENIED",
                "You do not have access to this organization.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidArgument(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ApiProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "VALIDATION_FAILED",
                "One or more fields are invalid.");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "code", validationCode(error.getCode()),
                        "message", safeValidationMessage(error.getCode())))
                .toList());
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableMessage(HttpMessageNotReadableException exception) {
        return ApiProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "MALFORMED_REQUEST",
                "The request body is malformed.");
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail idempotencyConflict(IdempotencyConflictException exception) {
        return ApiProblemDetails.create(
                HttpStatus.CONFLICT,
                "Idempotency conflict",
                "IDEMPOTENCY_CONFLICT",
                "The idempotency key was already used for a different request.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidValue(IllegalArgumentException exception) {
        return ApiProblemDetails.create(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "INVALID_REQUEST",
                "A request value is invalid.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail internalError(Exception exception) {
        log.error(
                "Unhandled API error correlationId={} exceptionType={}",
                MDC.get("correlationId"),
                exception.getClass().getSimpleName());
        return ApiProblemDetails.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal error",
                "INTERNAL_ERROR",
                "The request could not be completed.");
    }

    private String validationCode(String code) {
        if (code == null || !code.matches("[A-Za-z][A-Za-z0-9]{0,63}")) {
            return "INVALID";
        }
        return code.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT);
    }

    private String safeValidationMessage(String code) {
        return switch (java.util.Objects.toString(code, "")) {
            case "NotBlank" -> "Must not be blank.";
            case "NotEmpty" -> "Must not be empty.";
            case "NotNull" -> "Is required.";
            case "Size" -> "Has an invalid length.";
            case "Min", "Max", "DecimalMin", "DecimalMax" -> "Is outside the allowed range.";
            case "Email" -> "Must be a valid email address.";
            case "Pattern" -> "Has an invalid format.";
            default -> "Invalid value.";
        };
    }
}
