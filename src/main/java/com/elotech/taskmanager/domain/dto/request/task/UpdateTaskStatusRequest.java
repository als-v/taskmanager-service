package com.elotech.taskmanager.domain.dto.request.task;

import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @Schema(description = "Novo status da task", example = "IN_PROGRESS")
        @NotNull(message = "Task status is required")
        TaskStatus status
) {
}
