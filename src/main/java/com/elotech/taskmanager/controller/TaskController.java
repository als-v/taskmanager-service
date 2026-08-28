package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.request.task.CreateTaskRequest;
import com.elotech.taskmanager.domain.dto.request.task.UpdateTaskRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.task.TaskResponse;
import com.elotech.taskmanager.domain.error.ApiProblem;
import com.elotech.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@Tag(name = "Tasks", description = "CRUD de tasks dentro de projetos acessíveis ao usuário autenticado")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(
            summary = "Listar tasks do projeto",
            description = "Retorna uma página de tasks do projeto quando o usuário autenticado é membro `ADMIN` ou `MEMBER`. A ordenação é fixa por `createdAt DESC`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de tasks", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "tasks-page", value = """
                    {
                        "content": [
                            {
                                "id": "2f2a9a6b-4dc8-45c0-9a3f-8e7d36f0ad91",
                                "project_id": "8d1c1dc4-336c-4ce4-82ac-bffb82c94f5c",
                                "title": "Implementar autenticação JWT",
                                "description": "Criar login e refresh token",
                                "status": "TODO",
                                "priority": "HIGH",
                                "assignee_id": "0d949a6f-6dd3-49c5-b0ff-7e7f69dcb192",
                                "due_date": "2026-02-15T18:00:00",
                                "created_at": "2026-01-10T09:00:00",
                                "updated_at": "2026-01-10T09:00:00"
                            }
                        ],
                        "page": 0,
                        "size": 20,
                        "total_elements": 1,
                        "total_pages": 1
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "Paginação inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou inacessível", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    })
    public PageResponse<TaskResponse> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return taskService.list(projectId, page, size);
    }

    @GetMapping("/{taskId}")
    @Operation(
            summary = "Buscar task por ID",
            description = "Retorna uma task específica quando o usuário autenticado é membro do projeto e a task pertence ao projeto informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "404", description = "Projeto ou task inexistente/inacessível", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class), examples = @ExampleObject(name = "task-not-found", value = """
                    {
                        "status": 404,
                        "type": "about:blank",
                        "title": "error.task.not-found",
                        "detail": "Task not found",
                        "instance": "/api/projects/8d1c1dc4-336c-4ce4-82ac-bffb82c94f5c/tasks/2f2a9a6b-4dc8-45c0-9a3f-8e7d36f0ad91",
                        "code": "error.task.not-found"
                    }
                    """)))
    })
    public TaskResponse get(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        return taskService.get(projectId, taskId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Criar task",
            description = "Cria uma task no projeto quando o usuário autenticado é membro `ADMIN` ou `MEMBER`. O responsável, quando informado, deve ser membro do projeto."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task criada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class), examples = @ExampleObject(name = "invalid-task", value = """
                    {
                        "status": 400,
                        "type": "about:blank",
                        "title": "error.validation.failed",
                        "detail": "One or more fields are invalid",
                        "instance": "/api/projects/8d1c1dc4-336c-4ce4-82ac-bffb82c94f5c/tasks",
                        "code": "error.validation.failed",
                        "errors": [
                            {
                                "field": "title",
                                "code": "error.validation.NotBlank",
                                "message": "Task title is required"
                            }
                        ]
                    }
                    """))),
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou inacessível", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    })
    public TaskResponse create(@PathVariable UUID projectId, @Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(projectId, request);
    }

    @PatchMapping("/{taskId}")
    @Operation(
            summary = "Atualizar task",
            description = "Atualiza parcialmente uma task quando o usuário autenticado é membro `ADMIN` ou `MEMBER` do projeto. O payload deve informar ao menos um campo alterável."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task atualizada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class), examples = @ExampleObject(name = "empty-patch", value = """
                    {
                        "status": 400,
                        "type": "about:blank",
                        "title": "error.task.no-fields",
                        "detail": "At least one field must be provided",
                        "instance": "/api/projects/8d1c1dc4-336c-4ce4-82ac-bffb82c94f5c/tasks/2f2a9a6b-4dc8-45c0-9a3f-8e7d36f0ad91",
                        "code": "error.task.no-fields"
                    }
                    """))),
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "404", description = "Projeto ou task inexistente/inacessível", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    })
    public TaskResponse patch(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.patch(projectId, taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Deletar task",
            description = "Remove uma task do projeto. Somente membros `ADMIN` do projeto podem deletar tasks."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deletada"),
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class))),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado é membro, mas não é ADMIN do projeto", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class), examples = @ExampleObject(name = "delete-admin-required", value = """
                    {
                        "status": 403,
                        "type": "about:blank",
                        "title": "error.task.delete-admin-required",
                        "detail": "Only project admins can delete tasks",
                        "instance": "/api/projects/8d1c1dc4-336c-4ce4-82ac-bffb82c94f5c/tasks/2f2a9a6b-4dc8-45c0-9a3f-8e7d36f0ad91",
                        "code": "error.task.delete-admin-required"
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "Projeto ou task inexistente/inacessível", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    })
    public void delete(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        taskService.delete(projectId, taskId);
    }
}
