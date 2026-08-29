package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.request.project.CreateProjectRequest;
import com.elotech.taskmanager.domain.dto.request.project.UpdateProjectRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.project.ProjectResponse;
import com.elotech.taskmanager.domain.error.ApiProblem;
import com.elotech.taskmanager.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/projects")
@Tag(name = "Projetos", description = "Criação, consulta e edição de projetos com acesso por membership")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(
            summary = "Listar projetos do usuário autenticado",
            description = """
                    Retorna somente projetos em que o usuário autenticado participa como `ADMIN` ou `MEMBER`.

                    Regras da listagem:
                    - Paginação é 0-based (`page=0` representa a primeira página).
                    - `page` padrão: `0`.
                    - `size` padrão: `20`.
                    - `size` máximo: `100`; valores acima disso são limitados para `100`.
                    - Ordenação fixa por `created_at DESC`; parâmetro `sort` ainda não é suportado.
                    """
    )
            @ApiResponse(responseCode = "200", description = "Página de projetos acessíveis ao usuário autenticado",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            name = "projects-page",
                            summary = "Página de projetos",
                            value = """
                                    {
                                      "content": [
                                        {
                                          "id": "2f2a9a6b-4dc8-45c0-9a3f-8e7d36f0ad91",
                                          "name": "Plataforma interna",
                                          "description": "Backlog da equipe de desenvolvimento",
                                          "owner_id": "0d949a6f-6dd3-49c5-b0ff-7e7f69dcb192",
                                          "current_user_role": "ADMIN",
                                          "created_at": "2026-01-10T09:00:00",
                                          "updated_at": "2026-01-10T09:00:00"
                                        }
                                      ],
                                      "page": 0,
                                      "size": 20,
                                      "total_elements": 1,
                                      "total_pages": 1
                                    }
                                    """
                    )))
            @ApiResponse(responseCode = "400", description = "Paginação inválida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "invalid-page",
                                    summary = "Página inválida",
                                    value = """
                                            {
                                              "status": 400,
                                              "type": "about:blank",
                                              "title": "error.pagination.page-invalid",
                                              "detail": "Page must be greater than or equal to zero",
                                              "instance": "/api/projects",
                                              "code": "error.pagination.page-invalid"
                                            }
                                            """
                            )))
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    public PageResponse<ProjectResponse> list(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size) {
        return projectService.list(page, size);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar projeto por ID",
            description = """
                    Retorna os dados de um projeto específico somente quando o usuário autenticado é membro dele.

                    Para evitar exposição indevida de IDs, projetos inexistentes e projetos existentes mas inacessíveis
                    para o usuário atual retornam o mesmo status `404`.
                    """
    )
            @ApiResponse(responseCode = "200", description = "Projeto encontrado e acessível",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectResponse.class)))
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou inacessível",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "project-not-found",
                                    summary = "Projeto não encontrado ou inacessível",
                                    value = """
                                            {
                                              "status": 404,
                                              "type": "about:blank",
                                              "title": "error.project.not-found",
                                              "detail": "Project not found",
                                              "instance": "/api/projects/2f2a9a6b-4dc8-45c0-9a3f-8e7d36f0ad91",
                                              "code": "error.project.not-found"
                                            }
                                            """
                            )))
    public ProjectResponse get(@PathVariable UUID id) {
        return projectService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Criar projeto",
            description = """
                    Cria um novo projeto e registra automaticamente o usuário autenticado como membro `ADMIN`.

                    """
    )
            @ApiResponse(responseCode = "201", description = "Projeto criado com o usuário autenticado como ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectResponse.class)))
            @ApiResponse(responseCode = "400", description = "Payload inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "invalid-project",
                                    summary = "Nome obrigatório ausente",
                                    value = """
                                            {
                                              "status": 400,
                                              "type": "about:blank",
                                              "title": "error.validation.failed",
                                              "detail": "One or more fields are invalid",
                                              "instance": "/api/projects",
                                              "code": "error.validation.failed",
                                              "errors": [
                                                {
                                                  "field": "name",
                                                  "code": "error.validation.name.NotBlank",
                                                  "message": "Project name is required"
                                                }
                                              ]
                                            }
                                            """
                            )))
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Atualizar projeto",
            description = """
                    Atualiza parcialmente os dados cadastrais do projeto.

                    Regras:
                    - Somente membros `ADMIN` do projeto podem editar.
                    - Membros `MEMBER` recebem `403`.
                    - O payload deve informar ao menos um campo alterável.
                    """
    )
            @ApiResponse(responseCode = "200", description = "Projeto atualizado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectResponse.class)))
            @ApiResponse(responseCode = "400", description = "Payload inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "empty-patch",
                                    summary = "Nenhum campo alterável informado",
                                    value = """
                                            {
                                              "status": 400,
                                              "type": "about:blank",
                                              "title": "error.project.no-fields",
                                              "detail": "At least one field must be provided",
                                              "instance": "/api/projects/2f2a9a6b-4dc8-45c0-9a3f-8e7d36f0ad91",
                                              "code": "error.project.no-fields"
                                            }
                                            """
                            )))
            @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
            @ApiResponse(responseCode = "403", description = "Usuário autenticado é membro, mas não é ADMIN do projeto",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "admin-required",
                                    summary = "Apenas ADMIN pode editar",
                                    value = """
                                            {
                                              "status": 403,
                                              "type": "about:blank",
                                              "title": "error.project.admin-required",
                                              "detail": "Only project admins can perform this action",
                                              "instance": "/api/projects/2f2a9a6b-4dc8-45c0-9a3f-8e7d36f0ad91",
                                              "code": "error.project.admin-required"
                                            }
                                            """
                            )))
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou inacessível",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class)))
    public ProjectResponse patch(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.patch(id, request);
    }
}
