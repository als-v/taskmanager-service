package com.elotech.taskmanager.domain.dto.response.audit;

import com.elotech.taskmanager.domain.entity.TaskLog;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        @Schema(description = "Identificador unico do log")
        UUID id,
        @Schema(description = "Projeto ao qual o log pertence")
        UUID projectId,
        @Schema(description = "Task afetada pelo evento")
        UUID taskId,
        @Schema(description = "Usuario que executou a acao")
        UUID actorId,
        @Schema(description = "Acao auditada", example = "STATUS_CHANGED")
        AuditAction action,
        @Schema(description = "Status anterior, preenchido somente para alteracao de status")
        TaskStatus fromStatus,
        @Schema(description = "Novo status, preenchido somente para alteracao de status")
        TaskStatus toStatus,
        @Schema(description = "Data de criacao do log")
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(TaskLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getProjectId(),
                log.getTaskId(),
                log.getActorId(),
                log.getAction(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getCreatedAt()
        );
    }
}
