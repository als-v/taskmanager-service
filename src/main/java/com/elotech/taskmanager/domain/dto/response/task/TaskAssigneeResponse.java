package com.elotech.taskmanager.domain.dto.response.task;

import com.elotech.taskmanager.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record TaskAssigneeResponse(
        @Schema(description = "Identificador do usuário responsável")
        UUID id,

        @Schema(description = "Nome do usuário responsável", example = "Maria Silva")
        String name,

        @Schema(description = "E-mail do usuário responsável", example = "maria@example.com")
        String email
) {
    public static TaskAssigneeResponse from(User user) {
        if (user == null) {
            return null;
        }

        return new TaskAssigneeResponse(user.getId(), user.getName(), user.getEmail());
    }
}
