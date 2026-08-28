package com.elotech.taskmanager.domain.dto.request.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @Schema(description = "Nome visível do projeto. Obrigatório na criação.", example = "Plataforma interna", maxLength = 150)
        @NotBlank(message = "Project name is required")
        @Size(max = 150, message = "Project name must have at most 150 characters")
        String name,

        @Schema(description = "Descrição opcional do projeto.", example = "Backlog da equipe de desenvolvimento", maxLength = 2000)
        @Size(max = 2000, message = "Project description must have at most 2000 characters")
        String description
) {
}
