package com.elotech.taskmanager.domain.dto.request.task;

import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateTaskRequest(
        @Schema(description = "Título da task. Campo opcional no PATCH; quando informado, não pode ser vazio.", example = "Implementar autenticação JWT", maxLength = 200)
        @Size(max = 200, message = "Task title must at most 200 characters")
        String title,

        @Schema(description = "Descrição opcional da task", example = "Criar login, refresh token e proteção de rotas", maxLength = 2000)
        @Size(max = 2000, message = "Task description must at most 2000 characters")
        String description,

        @Schema(description = "Novo status da task", example = "IN_PROGRESS")
        TaskStatus status,

        @Schema(description = "Nova prioridade da task", example = "CRITICAL")
        Priority priority,

        @Schema(description = "Novo responsável pela task. Use null para remover o responsável.", example = "0d949a6f-6dd3-49c5-b0ff-7e7f69dcb192")
        UUID assigneeId,

        @Schema(description = "Novo prazo da task. Use null para remover o prazo.", example = "2026-02-20T18:00:00")
        LocalDateTime dueDate
) {
    public boolean hasNoChanges() {
        return title == null
                && description == null
                && status == null
                && priority == null
                && assigneeId == null
                && dueDate == null;
    }
}
