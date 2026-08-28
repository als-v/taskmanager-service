package com.elotech.taskmanager.domain.dto.response.task;

import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
        @Schema(description = "Identificador único da task")
        UUID id,

        @Schema(description = "Projeto ao qual a task pertence")
        UUID projectId,

        @Schema(description = "Título da task", example = "Implementar autenticação JWT")
        String title,

        @Schema(description = "Descrição da task", example = "Criar login e refresh token")
        String description,

        @Schema(description = "Status atual da task", example = "TODO")
        TaskStatus status,

        @Schema(description = "Prioridade atual da task", example = "HIGH")
        Priority priority,

        @Schema(description = "Usuário responsável pela task")
        UUID assigneeId,

        @Schema(description = "Prazo da task")
        LocalDateTime dueDate,

        @Schema(description = "Data de criação")
        LocalDateTime createdAt,

        @Schema(description = "Data da última atualização")
        LocalDateTime updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getUserId(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
