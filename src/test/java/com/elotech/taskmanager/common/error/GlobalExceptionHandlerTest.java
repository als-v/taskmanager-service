package com.elotech.taskmanager.common.error;

import com.elotech.taskmanager.web.auth.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        webRequest = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/auth/login"));
    }

    @Test
    void handleApiException_shouldMapStatusAndCode_forConflictException() {
        ConflictException ex = new ConflictException("error.auth.email-in-use", "Email already in use: maria@exemplo.com");

        ResponseEntity<ApiProblem> response = handler.handleApiException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo("error.auth.email-in-use");
        assertThat(response.getBody().getDetail()).isEqualTo("Email already in use: maria@exemplo.com");
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/auth/login");
    }

    @Test
    void handleApiException_shouldMapStatusAndCode_forUnauthorizedException() {
        UnauthorizedException ex = new UnauthorizedException("error.auth.invalid-credentials", "Invalid email or password");

        ResponseEntity<ApiProblem> response = handler.handleApiException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getCode()).isEqualTo("error.auth.invalid-credentials");
    }

    @Test
    void handleNotFound_shouldReturn404WithResourceCode() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "static/missing.js");

        ResponseEntity<ApiProblem> response = handler.handleNotFound(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo("error.resource.not-found");
        assertThat(response.getBody().getDetail()).contains("static/missing.js");
    }

    @Test
    void handleValidation_shouldConvertFieldToSnakeCaseAndBuildErrors() throws NoSuchMethodException {
        Method method = AuthController.class.getMethod("refresh", com.elotech.taskmanager.web.auth.dto.RefreshRequest.class);
        BindingResult bindingResult = new MapBindingResult(new HashMap<>(), "refreshRequest");
        bindingResult.addError(new FieldError("refreshRequest", "refreshToken", null, false,
                new String[]{"NotBlank"}, null, "Refresh token is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(method, 0), bindingResult);

        ResponseEntity<ApiProblem> response = handler.handleValidation(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiProblem body = response.getBody();
        assertThat(body.getCode()).isEqualTo("error.validation.failed");
        assertThat(body.getErrors()).hasSize(1);
        ApiProblem.ValidationError error = body.getErrors().get(0);
        assertThat(error.getField()).isEqualTo("refresh_token");
        assertThat(error.getCode()).isEqualTo("error.validation.refresh_token.NotBlank");
        assertThat(error.getMessage()).isEqualTo("Refresh token is required");
    }

    @Test
    void handleUnexpected_shouldReturn500WithGenericMessage() {
        RuntimeException ex = new RuntimeException("boom, leaked internal detail");

        ResponseEntity<ApiProblem> response = handler.handleUnexpected(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo("error.internal");
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
    }
}
