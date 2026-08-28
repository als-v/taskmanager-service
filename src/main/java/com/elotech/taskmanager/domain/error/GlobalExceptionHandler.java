package com.elotech.taskmanager.domain.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiProblem> handleApiException(ApiException ex, WebRequest request) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiProblem> handleNotFound(NoResourceFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, "error.resource.not-found",
                "Resource not found: " + ex.getResourcePath(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblem> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<ApiProblem.ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> {
                    String field = toSnakeCase(fieldError.getField());
                    return new ApiProblem.ValidationError(
                            field,
                            "error.validation." + field + "." + fieldError.getCode(),
                            fieldError.getDefaultMessage());
                })
                .toList();

        ApiProblem problem = ApiProblem.of(HttpStatus.BAD_REQUEST, "error.validation.failed",
                "One or more fields are invalid", uri(request));
        problem.setErrors(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> handleUnexpected(Exception ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiProblem> build(HttpStatus status, String code, String detail, WebRequest request) {
        return ResponseEntity.status(status).body(ApiProblem.of(status, code, detail, uri(request)));
    }

    private String uri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "";
    }
}
