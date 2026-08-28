package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.response.auth.AuthResponse;
import com.elotech.taskmanager.domain.dto.request.auth.LoginRequest;
import com.elotech.taskmanager.domain.dto.request.auth.RefreshRequest;
import com.elotech.taskmanager.domain.dto.request.auth.SignUpRequest;
import com.elotech.taskmanager.domain.dto.response.auth.UserResponse;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void signUp_shouldReturn201WithUserResponse() throws Exception {
        UUID id = UUID.randomUUID();
        given(authService.signUp(any(SignUpRequest.class)))
                .willReturn(new UserResponse(id, "Maria Silva", "maria@exemplo.com"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignUpRequest("Maria Silva", "maria@exemplo.com", "senha123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"));
    }

    @Test
    void signUp_shouldReturn400_whenFieldsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorMessages.VALIDATION_FAILED_CODE));
    }

    @Test
    void login_shouldReturn200WithAuthResponseInSnakeCase() throws Exception {
        UUID id = UUID.randomUUID();
        given(authService.login(any(LoginRequest.class))).willReturn(
                new AuthResponse("access-token", "refresh-token", "Bearer", new UserResponse(id, "Maria Silva", "maria@exemplo.com")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("maria@exemplo.com", "senha123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.refresh_token").value("refresh-token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("maria@exemplo.com"));
    }

    @Test
    void login_shouldReturn400_whenBodyMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorMessages.VALIDATION_FAILED_CODE));
    }

    @Test
    void refresh_shouldReturn200WithNewTokenPair() throws Exception {
        UUID id = UUID.randomUUID();
        given(authService.refresh(any(RefreshRequest.class))).willReturn(
                new AuthResponse("new-access-token", "new-refresh-token", "Bearer", new UserResponse(id, "Maria Silva", "maria@exemplo.com")));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("old-refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new-access-token"))
                .andExpect(jsonPath("$.refresh_token").value("new-refresh-token"));
    }

    @Test
    void refresh_shouldReturn400WithSnakeCaseFieldName_whenRefreshTokenMissing() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("refresh_token"))
                .andExpect(jsonPath("$.errors[0].code").value("error.validation.refresh_token.NotBlank"));
    }

    @Test
    void logout_shouldReturn204_whenValid() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("some-token"))))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(RefreshRequest.class));
    }

    @Test
    void logout_shouldReturn400_whenRefreshTokenMissing() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorMessages.VALIDATION_FAILED_CODE));
    }
}
