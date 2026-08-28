package com.elotech.taskmanager.domain.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiProblem extends ProblemDetail {

    private String code;
    private List<ValidationError> errors;

    public ApiProblem(HttpStatus status) {
        super(status.value());
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    public static ApiProblem of(HttpStatus status, String code, String detail, String instance) {
        ApiProblem problem = new ApiProblem(status);
        problem.setCode(code);
        problem.setTitle(code);
        problem.setDetail(detail);
        if (instance != null && !instance.isBlank()) {
            try {
                problem.setInstance(URI.create(instance));
            } catch (IllegalArgumentException ignored) {
                // leave instance unset
            }
        }
        return problem;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String field;
        private final String code;
        private final String message;

        public ValidationError(String field, String code, String message) {
            this.field = field;
            this.code = code;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
