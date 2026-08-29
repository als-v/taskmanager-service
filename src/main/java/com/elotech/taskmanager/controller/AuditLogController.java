package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.response.audit.AuditLogResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.error.ApiProblem;
import com.elotech.taskmanager.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/logs")
@Tag(name = "Audit log", description = "Consulta de eventos auditados por projeto")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(
            summary = "Listar logs do projeto",
            description = "Retorna eventos auditados quando o usuario autenticado e membro do projeto. Filtros aceitos: task_id, actor_id e action. Ordenacao fixa por created_at DESC."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logs encontrados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class), examples = @ExampleObject(name = "audit-log-page", value = """
                    {
                        "content": [
                            {
                                "id": "92d629f1-2d53-4a43-b7be-2660ab4a60d4",
                                "project_id": "8d1c1dc4-336c-4ce4-82ac-bffb82c94f5c",
                                "task_id": "2f2a9a6b-4dc8-45c0-9a3f-8e7d36f0ad91",
                                "actor_id": "0d949a6f-6dd3-49c5-b0ff-7e7f69dcb192",
                                "action": "STATUS_CHANGED",
                                "from_status": "TODO",
                                "to_status": "IN_PROGRESS",
                                "created_at": "2026-01-10T09:00:00"
                            }
                        ],
                        "page": 0,
                        "size": 20,
                        "total_elements": 1,
                        "total_pages": 1
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "Parametro de paginacao, UUID ou enum invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou inacessivel", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class)))
    })
    public PageResponse<AuditLogResponse> list(
            @Parameter(description = "Identificador do projeto") @PathVariable UUID projectId,
            @Parameter(description = "Task filtrada") @RequestParam(name = "task_id", required = false) UUID taskId,
            @Parameter(description = "Usuario que executou a acao") @RequestParam(name = "actor_id", required = false) UUID actorId,
            @Parameter(description = "Acao auditada") @RequestParam(required = false) AuditAction action,
            @Parameter(description = "Pagina solicitada, iniciando em 0") @RequestParam(required = false) Integer page,
            @Parameter(description = "Itens por pagina. Valores acima de 100 sao limitados para 100") @RequestParam(required = false) Integer size
    ) {
        return auditLogService.list(projectId, taskId, actorId, action, page, size);
    }
}
