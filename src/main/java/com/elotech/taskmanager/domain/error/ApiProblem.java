package com.elotech.taskmanager.domain.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApiProblem that = (ApiProblem) o;

        return Objects.equals(getCode(), that.getCode())
                && Objects.equals(getDetail(), that.getDetail())
                && Objects.equals(getInstance(), that.getInstance())
                && Objects.equals(getStatus(), that.getStatus())
                && Objects.equals(getType(), that.getType())
                && Objects.equals(getTitle(), that.getTitle())
                && Objects.equals(getProperties(), that.getProperties())
                && Objects.equals(code, that.code)
                && Objects.equals(errors, that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCode(), getDetail(), getInstance(), getStatus(), getType(), getTitle(),
                getProperties(), code, errors);
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
