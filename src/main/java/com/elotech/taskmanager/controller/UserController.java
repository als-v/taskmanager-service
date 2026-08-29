package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.response.auth.UserResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.error.ApiProblem;
import com.elotech.taskmanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/users")
@Tag(name = "Usuarios", description = "Busca de usuarios autenticados para selecao e atribuicao")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(
            summary = "Listar usuarios",
            description = """
                    Retorna usuarios paginados sem expor senha.

                    Filtros aceitos:
                    - name: busca parcial case-insensitive no nome.
                    - email: busca parcial case-insensitive no e-mail.
                    - project_id: quando informado, exige membership no projeto e retorna apenas usuarios que ainda nao pertencem a ele.

                    Quando name e email sao informados juntos, os filtros sao combinados com AND.
                    A paginacao usa page default 0, size default 20 e limite maximo 100.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Usuarios listados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class), examples = @ExampleObject(name = "users-page", value = """
            {
                "content": [
                    {
                        "id": "23ccfc7d-de3f-46ea-8752-4dd71c14da75",
                        "name": "Usuario #2",
                        "email": "usuario2@usuario2.com"
                    }
                ],
                "page": 0,
                "size": 20,
                "total_elements": 1,
                "total_pages": 1
            }
            """)))
    @ApiResponse(responseCode = "400", description = "Parametro de paginacao ou UUID invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class), examples = @ExampleObject(name = "invalid-parameter", value = """
            {
                "status": 400,
                "type": "about:blank",
                "title": "error.request.parameter-invalid",
                "detail": "Invalid value for parameter: project_id",
                "instance": "/api/users",
                "code": "error.request.parameter-invalid"
            }
            """)))
    @ApiResponse(responseCode = "401", description = "Access token ausente, expirado ou invalido", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class)))
    @ApiResponse(responseCode = "404", description = "Projeto inexistente ou inacessivel quando project_id e informado", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class), examples = @ExampleObject(name = "project-not-found", value = """
            {
                "status": 404,
                "type": "about:blank",
                "title": "error.project.not-found",
                "detail": "Project not found",
                "instance": "/api/users",
                "code": "error.project.not-found"
            }
            """)))
    public PageResponse<UserResponse> list(
            @Parameter(description = "Pagina solicitada, iniciando em 0", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Itens por pagina. Valores acima de 100 sao limitados para 100", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Filtro parcial por nome", example = "Usuario")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filtro parcial por e-mail", example = "usuario2")
            @RequestParam(required = false) String email,
            @Parameter(description = "Projeto usado para retornar apenas usuarios que ainda nao sao membros", example = "b21f019e-5e79-4ffb-b647-815791e9c64c")
            @RequestParam(name = "project_id", required = false) UUID projectId
    ) {
        return userService.list(page, size, name, email, projectId);
    }
}
