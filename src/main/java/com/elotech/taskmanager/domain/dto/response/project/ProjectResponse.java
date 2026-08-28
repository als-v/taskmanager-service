package com.elotech.taskmanager.domain.dto.response.project;

import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
        @Schema(description = "Identificador único do projeto")
        UUID id,

        @Schema(description = "Nome do projeto", example = "Plataforma interna")
        String name,

        @Schema(description = "Descrição do projeto", example = "Backlog da equipe de desenvolvimento")
        String description,

        @Schema(description = "Usuário dono do projeto")
        UUID ownerId,

        @Schema(description = "Perfil do usuário autenticado neste projeto")
        MemberRole currentUserRole,

        @Schema(description = "Data de criação")
        LocalDateTime createdAt,

        @Schema(description = "Data da última atualização")
        LocalDateTime updatedAt
) {
    public static ProjectResponse from(Project project, MemberRole currentUserRole) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwnerId(),
                currentUserRole,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
