package com.elotech.taskmanager.domain.dto.request.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record UpdateTaskDueDateRequest(
        @Schema(description = "Novo prazo da task. Use null para remover o prazo.", example = "2026-02-20T18:00:00")
        LocalDateTime dueDate
) {
}
