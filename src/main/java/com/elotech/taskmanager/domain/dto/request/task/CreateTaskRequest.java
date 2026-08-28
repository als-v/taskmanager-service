package com.elotech.taskmanager.domain.dto.request.task;

import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateTaskRequest(
        @Schema(description = "Título da task", example = "Implementar autenticação JWT", maxLength = 200)
        @NotBlank(message = "Task title is required")
        @Size(max = 200, message = "Task title must at most 200 characters")
        String title,

        @Schema(description = "Descrição opcional da task", example = "Criar login e refresh token", maxLength = 2000)
        @Size(max = 2000, message = "Task description must at most 2000 characters")
        String description,

        @Schema(description = "Status inicial da task", example = "TODO", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Task status is required")
        TaskStatus status,

        @Schema(description = "Prioridade inicial da task", example = "HIGH", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Task priority is required")
        Priority priority,

        @Schema(description = "Usuário responsável pela task. Opcional.", example = "0d949a6f-6dd3-49c5-b0ff-7e7f69dcb192")
        UUID assigneeId,

        @Schema(description = "Prazo da task. Opcional.", example = "2026-02-15T18:00:00")
        LocalDateTime dueDate
) {
}
