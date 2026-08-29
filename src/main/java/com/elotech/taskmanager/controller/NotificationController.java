package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.notification.NotificationResponse;
import com.elotech.taskmanager.domain.error.ApiProblem;
import com.elotech.taskmanager.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificacoes", description = "Consulta e leitura de notificacoes do usuario autenticado")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(
            summary = "Listar notificacoes",
            description = "Retorna notificacoes distribuidas para o usuario autenticado. O filtro unread aceita true ou false. Ordenacao fixa por created_at DESC."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacoes encontradas", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class), examples = @ExampleObject(name = "notifications-page", value = """
                    {
                        "content": [
                            {
                                "id": "92d629f1-2d53-4a43-b7be-2660ab4a60d4",
                                "type": "TASK_ASSIGNED",
                                "message": "Voce foi atribuido a uma task",
                                "project_id": "8d1c1dc4-336c-4ce4-82ac-bffb82c94f5c",
                                "task_id": "2f2a9a6b-4dc8-45c0-9a3f-8e7d36f0ad91",
                                "created_by": "0d949a6f-6dd3-49c5-b0ff-7e7f69dcb192",
                                "created_at": "2026-01-10T09:00:00",
                                "read_at": null,
                                "unread": true
                            }
                        ],
                        "page": 0,
                        "size": 20,
                        "total_elements": 1,
                        "total_pages": 1
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "Parametro de paginacao ou boolean invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class)))
    })
    public PageResponse<NotificationResponse> list(
            @Parameter(description = "Filtra notificacoes por estado de leitura") @RequestParam(required = false) Boolean unread,
            @Parameter(description = "Pagina solicitada, iniciando em 0") @RequestParam(required = false) Integer page,
            @Parameter(description = "Itens por pagina. Valores acima de 100 sao limitados para 100") @RequestParam(required = false) Integer size
    ) {
        return notificationService.list(unread, page, size);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(
            summary = "Marcar notificacao como lida",
            description = "Marca como lida uma notificacao distribuida para o usuario autenticado. Se ja estiver lida, retorna a notificacao sem erro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacao lida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationResponse.class))),
            @ApiResponse(responseCode = "400", description = "UUID invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "404", description = "Notificacao inexistente para o usuario autenticado", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class)))
    })
    public NotificationResponse markAsRead(
            @Parameter(description = "Identificador da notificacao") @PathVariable UUID notificationId
    ) {
        return notificationService.markAsRead(notificationId);
    }
}
