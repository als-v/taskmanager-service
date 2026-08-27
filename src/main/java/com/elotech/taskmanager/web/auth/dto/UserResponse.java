package com.elotech.taskmanager.web.auth.dto;

import com.elotech.taskmanager.domain.entities.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record UserResponse(
        @Schema(description = "Identificador único do usuário")
        UUID id,

        @Schema(description = "Nome completo do usuário", example = "Maria Silva")
        String name,

        @Schema(description = "E-mail do usuário", example = "usuario@exemplo.com")
        String email
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
