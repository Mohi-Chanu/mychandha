package com.mychandha.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mychandha.platform.idempotency.IdempotencyConflictException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void emitsStableProblemDetailsWithoutExceptionText() {
        MDC.put("correlationId", "safe-correlation");

        var problem = handler.idempotencyConflict(new IdempotencyConflictException());

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getProperties())
                .containsEntry("code", "IDEMPOTENCY_CONFLICT")
                .containsEntry("correlationId", "safe-correlation");
        assertThat(problem.getDetail())
                .isEqualTo("The idempotency key was already used for a different request.");
    }

    @Test
    void validationErrorsUseImmutableSafeShape() throws Exception {
        ValidationInput input = new ValidationInput();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(input, "input");
        binding.rejectValue("name", "NotBlank", "must not be blank");
        Method method = ApiExceptionHandlerTest.class
                .getDeclaredMethod("validationTarget", String.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), binding);

        var problem = handler.invalidArgument(exception);

        assertThat(problem.getProperties()).containsEntry("code", "VALIDATION_FAILED");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors =
                (List<Map<String, String>>) problem.getProperties().get("errors");
        assertThat(errors).containsExactly(Map.of(
                "field", "name",
                "code", "NOT_BLANK",
                "message", "Must not be blank."));
        assertThatThrownBy(errors::clear)
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void internalErrorsReturnGenericDetail(CapturedOutput output) {
        var problem = handler.internalError(
                new IllegalStateException("token=secret-value"));

        assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_ERROR");
        assertThat(problem.getDetail()).doesNotContain("secret-value");
        assertThat(output).doesNotContain("secret-value");
    }

    @SuppressWarnings("unused")
    private void validationTarget(String value) {
    }

    private static final class ValidationInput {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
