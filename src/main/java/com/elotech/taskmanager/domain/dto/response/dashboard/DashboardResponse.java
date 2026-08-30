package com.elotech.taskmanager.domain.dto.response.dashboard;

import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DashboardResponse(
        @Schema(description = "Total de projetos dos quais o usuario autenticado participa", example = "4")
        long projectsTotal,

        @Schema(description = "Total de tasks consideradas no escopo do dashboard", example = "38")
        long tasksTotal,

        @Schema(description = "Total de tasks por status")
        Map<TaskStatus, Long> byStatus,

        @Schema(description = "Total de tasks por prioridade")
        Map<Priority, Long> byPriority,

        @Schema(description = "Projetos acessiveis ao usuario autenticado")
        List<DashboardProjectResponse> projects,

        @Schema(description = "Projeto aplicado como filtro, ou null quando sem filtro")
        UUID selectedProjectId,

        @Schema(description = "Data e hora em que o dashboard foi gerado")
        LocalDateTime generatedAt
) {
}
