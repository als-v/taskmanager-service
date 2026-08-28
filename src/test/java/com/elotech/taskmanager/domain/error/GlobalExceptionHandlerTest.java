package com.elotech.taskmanager.domain.error;

import com.elotech.taskmanager.domain.dto.request.auth.RefreshRequest;
import com.elotech.taskmanager.controller.AuthController;
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
        ConflictException ex = new ConflictException(ErrorMessages.AUTH_EMAIL_IN_USE_CODE, "Email already in use: maria@exemplo.com");

        ResponseEntity<ApiProblem> response = handler.handleApiException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorMessages.AUTH_EMAIL_IN_USE_CODE);
        assertThat(response.getBody().getDetail()).isEqualTo("Email already in use: maria@exemplo.com");
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/auth/login");
    }

    @Test
    void handleApiException_shouldMapStatusAndCode_forUnauthorizedException() {
        UnauthorizedException ex = new UnauthorizedException(ErrorMessages.AUTH_INVALID_CREDENTIALS_CODE, ErrorMessages.AUTH_INVALID_CREDENTIALS_MESSAGE);

        ResponseEntity<ApiProblem> response = handler.handleApiException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorMessages.AUTH_INVALID_CREDENTIALS_CODE);
    }

    @Test
    void handleNotFound_shouldReturn404WithResourceCode() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "static/missing.js");

        ResponseEntity<ApiProblem> response = handler.handleNotFound(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorMessages.RESOURCE_NOT_FOUND_CODE);
        assertThat(response.getBody().getDetail()).contains("static/missing.js");
    }

    @Test
    void handleValidation_shouldConvertFieldToSnakeCaseAndBuildErrors() throws NoSuchMethodException {
        Method method = AuthController.class.getMethod("refresh", RefreshRequest.class);
        BindingResult bindingResult = new MapBindingResult(new HashMap<>(), "refreshRequest");
        bindingResult.addError(new FieldError("refreshRequest", "refreshToken", null, false,
                new String[]{"NotBlank"}, null, "Refresh token is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(method, 0), bindingResult);

        ResponseEntity<ApiProblem> response = handler.handleValidation(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiProblem body = response.getBody();
        assertThat(body.getCode()).isEqualTo(ErrorMessages.VALIDATION_FAILED_CODE);
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
        assertThat(response.getBody().getCode()).isEqualTo(ErrorMessages.INTERNAL_CODE);
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
    }
}
