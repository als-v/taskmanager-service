package com.elotech.taskmanager.domain.dto.response.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record DashboardProjectResponse(
        @Schema(description = "Identificador unico do projeto")
        UUID id,

        @Schema(description = "Nome do projeto", example = "Plataforma interna")
        String name
) {
}
