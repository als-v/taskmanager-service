package com.elotech.taskmanager.web.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @Schema(description = "Nome completo do usuário", example = "Maria Silva")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "E-mail que será usado para login. Deve ser único.", example = "usuario@exemplo.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Senha de acesso, mínimo de 6 caracteres", example = "senha123", minLength = 6)
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
) {
}
