package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardResponse;
import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardWipResponse;
import com.elotech.taskmanager.domain.error.ApiProblem;
import com.elotech.taskmanager.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar dashboard",
            description = "Retorna os principais indicadores e os projetos acessiveis ao usuario autenticado. Quando project_id e informado, os indicadores de tasks ficam restritos ao projeto selecionado."
    )
    @ApiResponse(responseCode = "200", description = "Dashboard retornado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardResponse.class), examples = @ExampleObject(name = "dashboard", value = """
            {
              "projects_total": 4,
              "tasks_total": 38,
              "by_status": {"TODO": 10, "IN_PROGRESS": 11, "DONE": 17},
              "by_priority": {"LOW": 5, "MEDIUM": 14, "HIGH": 13, "CRITICAL": 6},
              "overdue": 4,
              "due_soon": 7,
              "projects": [{"id": "8d1c1dc4-336c-4ce4-82ac-bffb82c94f5c", "name": "Plataforma interna"}],
              "selected_project_id": null,
              "generated_at": "2026-08-29T10:00:00"
            }
            """)))
    @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou invalido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    @ApiResponse(responseCode = "404", description = "Projeto inexistente ou inacessivel", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    public DashboardResponse get(@RequestParam(name = "project_id", required = false) UUID projectId) {
        return dashboardService.getDashboard(projectId);
    }

    @GetMapping("/wip")
    @Operation(
            summary = "Consultar WIP por responsavel",
            description = "Retorna responsaveis com tasks IN_PROGRESS nos projetos acessiveis ao usuario autenticado. Quando project_id e informado, o agrupamento fica restrito ao projeto selecionado."
    )
    @ApiResponse(responseCode = "200", description = "WIP retornado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardWipResponse.class), examples = @ExampleObject(name = "dashboard-wip", value = """
            {
              "items": [
                {
                  "user_id": "9a8f6c39-3fd6-42e7-b6f9-7c0f5cba1d21",
                  "name": "Maria Silva",
                  "email": "maria@example.com",
                  "in_progress": 3
                }
              ],
              "selected_project_id": "8d1c1dc4-336c-4ce4-82ac-bffb82c94f5c",
              "generated_at": "2026-08-29T10:00:00"
            }
            """)))
    @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou invalido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    @ApiResponse(responseCode = "404", description = "Projeto inexistente ou inacessivel", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    public DashboardWipResponse getWip(@RequestParam(name = "project_id", required = false) UUID projectId) {
        return dashboardService.getWip(projectId);
    }
}
