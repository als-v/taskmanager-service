package com.elotech.taskmanager.domain.dto.response.notification;

import com.elotech.taskmanager.domain.entity.Notification;
import com.elotech.taskmanager.domain.enumeration.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        @Schema(description = "Identificador unico da notificacao")
        UUID id,
        @Schema(description = "Tipo da notificacao", example = "TASK_ASSIGNED")
        NotificationType type,
        @Schema(description = "Mensagem pronta para exibicao")
        String message,
        @Schema(description = "Projeto relacionado a notificacao")
        UUID projectId,
        @Schema(description = "Task relacionada a notificacao")
        UUID taskId,
        @Schema(description = "Usuario que gerou a notificacao")
        UUID createdBy,
        @Schema(description = "Data de criacao da notificacao")
        LocalDateTime createdAt,
        @Schema(description = "Data de leitura da notificacao para o usuario autenticado")
        LocalDateTime readAt,
        @Schema(description = "Indica se a notificacao ainda nao foi lida")
        boolean unread
) {
    public static NotificationResponse from(Notification notification, LocalDateTime readAt) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getProjectId(),
                notification.getTaskId(),
                notification.getCreatedBy(),
                notification.getCreatedAt(),
                readAt,
                readAt == null
        );
    }
}
