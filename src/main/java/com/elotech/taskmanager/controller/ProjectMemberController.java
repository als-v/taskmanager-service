package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.request.member.AddProjectMembersRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.member.ProjectMemberResponse;
import com.elotech.taskmanager.domain.error.ApiProblem;
import com.elotech.taskmanager.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
@Tag(name = "Membros do projeto", description = "Consulta e inclusao de membros em projetos")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @GetMapping
    @Operation(
            summary = "Listar membros do projeto",
            description = """
                    Retorna membros do projeto informado.

                    Regras:
                    - O usuario autenticado deve ser membro do projeto.
                    - Projeto inexistente e projeto inacessivel retornam 404.
                    - Nao existe filtro por role neste endpoint.

                    Filtros aceitos: name e email, ambos parciais e case-insensitive.
                    Quando enviados juntos, os filtros sao combinados com AND.
                    A paginacao usa page default 0, size default 20 e limite maximo 100.
                    A ordenacao e por nome e e-mail do usuario.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Membros listados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class), examples = @ExampleObject(name = "members-page", value = """
            {
                "content": [
                    {
                        "user_id": "0e853a32-91cb-4fb5-aa7e-392e866a7aa8",
                        "name": "Usuario #1",
                        "email": "usuario@usuario.com",
                        "role": "ADMIN",
                        "joined_at": "2026-08-28T14:42:40.181908"
                    }
                ],
                "page": 0,
                "size": 20,
                "total_elements": 1,
                "total_pages": 1
            }
            """)))
    @ApiResponse(responseCode = "400", description = "Parametro de paginacao ou UUID invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class), examples = @ExampleObject(name = "invalid-project-id", value = """
            {
                "status": 400,
                "type": "about:blank",
                "title": "error.request.parameter-invalid",
                "detail": "Invalid value for parameter: projectId",
                "instance": "/api/projects/23/members",
                "code": "error.request.parameter-invalid"
            }
            """)))
    @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class)))
    @ApiResponse(responseCode = "404", description = "Projeto inexistente ou inacessivel", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class)))
    public PageResponse<ProjectMemberResponse> list(
            @Parameter(description = "Identificador do projeto", example = "b21f019e-5e79-4ffb-b647-815791e9c64c")
            @PathVariable UUID projectId,
            @Parameter(description = "Pagina solicitada, iniciando em 0", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Itens por pagina. Valores acima de 100 sao limitados para 100", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Filtro parcial por nome do usuario", example = "Usuario")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filtro parcial por e-mail do usuario", example = "usuario")
            @RequestParam(required = false) String email
    ) {
        return projectMemberService.list(projectId, page, size, name, email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Adicionar membros ao projeto",
            description = """
                    Adiciona um ou mais usuarios existentes ao projeto como MEMBER.

                    Payload esperado:
                    - user_ids: lista obrigatoria de UUIDs validos, no formato padrao de 36 caracteres.
                    - Exemplo valido: {"user_ids":["23ccfc7d-de3f-46ea-8752-4dd71c14da75"]}
                    - Exemplo invalido: {"user_ids":["23"]}
                    - Campo role nao faz parte do contrato; todos os usuarios adicionados entram como MEMBER.

                    Regras:
                    - Somente ADMIN do projeto pode adicionar membros.
                    - A lista deve conter ao menos um usuario.
                    - Usuario duplicado no payload retorna 400.
                    - Usuario inexistente retorna 404.
                    - Usuario ja membro retorna 409.
                    - A operacao e atomica: se houver qualquer erro, nenhum membro e criado.
                    - Para cada usuario adicionado, e criada uma notificacao PROJECT_ADDED.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Lista de usuarios a adicionar como MEMBER",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AddProjectMembersRequest.class), examples = @ExampleObject(name = "add-members", value = """
                    {
                        "user_ids": [
                            "23ccfc7d-de3f-46ea-8752-4dd71c14da75",
                            "4a17a1a8-7692-4d0a-a967-8c0b3952ed7b"
                        ]
                    }
                    """))
    )
    @ApiResponse(responseCode = "201", description = "Membros adicionados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectMemberResponse.class), examples = @ExampleObject(name = "members-created", value = """
            [
                {
                    "user_id": "23ccfc7d-de3f-46ea-8752-4dd71c14da75",
                    "name": "Usuario #2",
                    "email": "usuario2@usuario2.com",
                    "role": "MEMBER",
                    "joined_at": "2026-08-28T17:22:27.360000"
                }
            ]
            """)))
    @ApiResponse(responseCode = "400", description = "Payload invalido, UUID mal formatado, lista vazia ou usuario duplicado no payload", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class), examples = {
            @ExampleObject(name = "invalid-uuid", summary = "UUID mal formatado", value = """
            {
                "status": 400,
                "type": "about:blank",
                "title": "error.request.body-invalid",
                "detail": "Request body is invalid or malformed",
                "instance": "/api/projects/b21f019e-5e79-4ffb-b647-815791e9c64c/members",
                "code": "error.request.body-invalid"
            }
            """),
            @ExampleObject(name = "empty-list", summary = "Lista vazia", value = """
            {
                "status": 400,
                "type": "about:blank",
                "title": "error.project-member.empty-list",
                "detail": "At least one user must be provided",
                "instance": "/api/projects/b21f019e-5e79-4ffb-b647-815791e9c64c/members",
                "code": "error.project-member.empty-list"
            }
            """),
            @ExampleObject(name = "duplicate-user", summary = "Usuario duplicado no payload", value = """
            {
                "status": 400,
                "type": "about:blank",
                "title": "error.project-member.duplicate-user",
                "detail": "Request contains duplicated users",
                "instance": "/api/projects/b21f019e-5e79-4ffb-b647-815791e9c64c/members",
                "code": "error.project-member.duplicate-user"
            }
            """)
    }))
    @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class)))
    @ApiResponse(responseCode = "403", description = "Usuario autenticado e membro, mas nao e ADMIN do projeto", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class), examples = @ExampleObject(name = "admin-required", value = """
            {
                "status": 403,
                "type": "about:blank",
                "title": "error.project.admin-required",
                "detail": "Only project admins can perform this action",
                "instance": "/api/projects/b21f019e-5e79-4ffb-b647-815791e9c64c/members",
                "code": "error.project.admin-required"
            }
            """)))
    @ApiResponse(responseCode = "404", description = "Projeto inacessivel ou usuario inexistente", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class), examples = @ExampleObject(name = "user-not-found", value = """
            {
                "status": 404,
                "type": "about:blank",
                "title": "error.user.not-found",
                "detail": "User not found",
                "instance": "/api/projects/b21f019e-5e79-4ffb-b647-815791e9c64c/members",
                "code": "error.user.not-found"
            }
            """)))
    @ApiResponse(responseCode = "409", description = "Algum usuario informado ja pertence ao projeto", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class), examples = @ExampleObject(name = "already-member", value = """
            {
                "status": 409,
                "type": "about:blank",
                "title": "error.project-member.already-exists",
                "detail": "User is already a project member",
                "instance": "/api/projects/b21f019e-5e79-4ffb-b647-815791e9c64c/members",
                "code": "error.project-member.already-exists"
            }
            """)))
    public List<ProjectMemberResponse> add(
            @Parameter(description = "Identificador do projeto", example = "b21f019e-5e79-4ffb-b647-815791e9c64c")
            @PathVariable UUID projectId,
            @RequestBody AddProjectMembersRequest request
    ) {
        return projectMemberService.add(projectId, request);
    }
}
