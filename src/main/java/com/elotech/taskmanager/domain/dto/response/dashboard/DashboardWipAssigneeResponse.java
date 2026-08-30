package com.elotech.taskmanager.domain.dto.response.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record DashboardWipAssigneeResponse(
        @Schema(description = "Identificador unico do responsavel")
        UUID userId,

        @Schema(description = "Nome do responsavel", example = "Maria Silva")
        String name,

        @Schema(description = "E-mail do responsavel", example = "maria@example.com")
        String email,

        @Schema(description = "Quantidade de tasks IN_PROGRESS atribuidas ao responsavel", example = "3")
        long inProgress
) {
}
