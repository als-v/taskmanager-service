package com.elotech.taskmanager.domain.dto.response.audit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record AuditLogReferenceResponse(
        @Schema(description = "Identificador do recurso referenciado")
        UUID id,
        @Schema(description = "Nome do recurso referenciado")
        String name
) {
}
