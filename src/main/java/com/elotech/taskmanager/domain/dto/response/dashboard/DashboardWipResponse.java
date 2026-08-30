package com.elotech.taskmanager.domain.dto.response.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DashboardWipResponse(
        @Schema(description = "Responsaveis com tasks IN_PROGRESS no escopo consultado")
        List<DashboardWipAssigneeResponse> items,

        @Schema(description = "Projeto aplicado como filtro, ou null quando sem filtro")
        UUID selectedProjectId,

        @Schema(description = "Data e hora em que o WIP foi gerado")
        LocalDateTime generatedAt
) {
}
