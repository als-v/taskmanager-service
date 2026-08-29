package com.elotech.taskmanager.domain.dto.request.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record UpdateTaskAssigneeRequest(
        @Schema(description = "Novo responsável pela task. Use null para remover o responsável.", example = "0d949a6f-6dd3-49c5-b0ff-7e7f69dcb192")
        UUID assigneeId
) {
}
