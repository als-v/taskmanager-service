package com.elotech.taskmanager.domain.dto.response.audit;

import com.elotech.taskmanager.domain.entity.TaskLog;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.repository.tasklog.TaskLogWithReferences;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        @Schema(description = "Identificador unico do log")
        UUID id,
        @Schema(description = "Projeto ao qual o log pertence")
        UUID projectId,
        @Schema(description = "Task afetada pelo evento")
        AuditLogReferenceResponse task,
        @Schema(description = "Usuario que executou a acao")
        AuditLogReferenceResponse actor,
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
                new AuditLogReferenceResponse(log.getTaskId(), null),
                new AuditLogReferenceResponse(log.getActorId(), null),
                log.getAction(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getCreatedAt()
        );
    }

    public static AuditLogResponse from(TaskLogWithReferences projection) {
        TaskLog log = projection.log();
        return new AuditLogResponse(
                log.getId(),
                log.getProjectId(),
                new AuditLogReferenceResponse(projection.task().getId(), projection.task().getTitle()),
                new AuditLogReferenceResponse(projection.actor().getId(), projection.actor().getName()),
                log.getAction(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getCreatedAt()
        );
    }
}
