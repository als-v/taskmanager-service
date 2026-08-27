package com.elotech.taskmanager.web.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "E-mail cadastrado do usuário", example = "usuario@exemplo.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Senha do usuário", example = "senha123")
        @NotBlank(message = "Password is required")
        String password
) {
}
