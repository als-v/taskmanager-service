package com.elotech.taskmanager.domain.dto.response.member;

import com.elotech.taskmanager.domain.enumeration.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectMemberResponse(
        @Schema(description = "Identificador do usuário membro do projeto")
        UUID userId,
        @Schema(description = "Nome do usuário", example = "Maria Silva")
        String name,
        @Schema(description = "E-mail do usuário", example = "maria@exemplo.com")
        String email,
        @Schema(description = "Papel do usuário no projeto", example = "MEMBER")
        MemberRole role,
        @Schema(description = "Data de entrada no projeto")
        LocalDateTime joinedAt
) {
}
