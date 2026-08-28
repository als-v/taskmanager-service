package com.elotech.taskmanager.domain.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiProblem> handleApiException(ApiException ex, WebRequest request) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiProblem> handleNotFound(NoResourceFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorMessages.RESOURCE_NOT_FOUND_CODE, ErrorMessages.RESOURCE_NOT_FOUND_MESSAGE + ex.getResourcePath(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiProblem> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorMessages.METHOD_NOT_ALLOWED_CODE, ErrorMessages.METHOD_NOT_ALLOWED_MESSAGE + ex.getMethod(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiProblem> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorMessages.REQUEST_PARAMETER_INVALID_CODE, ErrorMessages.REQUEST_PARAMETER_INVALID_MESSAGE + ex.getName(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiProblem> handleMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorMessages.REQUEST_BODY_INVALID_CODE, ErrorMessages.REQUEST_BODY_INVALID_MESSAGE, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblem> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<ApiProblem.ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> {
                    String field = toSnakeCase(fieldError.getField());
                    return new ApiProblem.ValidationError(field, "error.validation." + field + "." + fieldError.getCode(), fieldError.getDefaultMessage());
                })
                .toList();

        ApiProblem problem = ApiProblem.of(HttpStatus.BAD_REQUEST, ErrorMessages.VALIDATION_FAILED_CODE, ErrorMessages.VALIDATION_FAILED_MESSAGE, uri(request));
        problem.setErrors(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> handleUnexpected(Exception ex, WebRequest request) {
        if (log.isErrorEnabled()) {
            log.error("Unexpected error while handling request {}", uri(request), ex);
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.INTERNAL_CODE, ErrorMessages.INTERNAL_MESSAGE, request);
    }

    private ResponseEntity<ApiProblem> build(HttpStatus status, String code, String detail, WebRequest request) {
        return ResponseEntity.status(status).body(ApiProblem.of(status, code, detail, uri(request)));
    }

    private String uri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return null;
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
