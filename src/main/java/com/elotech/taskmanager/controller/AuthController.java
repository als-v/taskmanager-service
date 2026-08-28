package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.error.ApiProblem;
import com.elotech.taskmanager.domain.dto.request.auth.LoginRequest;
import com.elotech.taskmanager.domain.dto.request.auth.RefreshRequest;
import com.elotech.taskmanager.domain.dto.request.auth.SignUpRequest;
import com.elotech.taskmanager.domain.dto.response.auth.AuthResponse;
import com.elotech.taskmanager.domain.dto.response.auth.UserResponse;
import com.elotech.taskmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro, login e renovação de sessão via JWT (access token + refresh token)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(
            summary = "Criar conta de usuário",
            description = """
                    Cadastra um novo usuário com nome, e-mail e senha.
                    
                    - Não retorna nenhum token — após o cadastro, autentique-se em `POST /api/auth/login`.
                    - O e-mail deve ser único: uma segunda tentativa de cadastro com o mesmo e-mail é rejeitada.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já está em uso por outra conta",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "error.auth.email-in-use",
                                    summary = "E-mail já cadastrado",
                                    value = """
                                            {
                                              "status": 409,
                                              "type": "about:blank",
                                              "title": "error.auth.email-in-use",
                                              "detail": "Email already in use: usuario@exemplo.com",
                                              "instance": "/api/auth/signup",
                                              "code": "error.auth.email-in-use"
                                            }
                                            """))),
            @ApiResponse(responseCode = "400", description = "Um ou mais campos são inválidos (nome, e-mail ou senha)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "error.validation.failed",
                                    summary = "Campos inválidos",
                                    value = """
                                            {
                                              "status": 400,
                                              "type": "about:blank",
                                              "title": "error.validation.failed",
                                              "detail": "One or more fields are invalid",
                                              "instance": "/api/auth/signup",
                                              "code": "error.validation.failed",
                                              "errors": [
                                                {
                                                  "field": "email",
                                                  "code": "error.validation.email.NotBlank",
                                                  "message": "Email is required"
                                                }
                                              ]
                                            }
                                            """)))
    })
    public UserResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "Autenticar usuário",
            description = """
                    Autentica com e-mail e senha e, se válidos, retorna um par de tokens JWT.
                    
                    - `access_token`: usado no header `Authorization: Bearer {access_token}` para acessar rotas protegidas. Vida curta.
                    - `refresh_token`: usado em `POST /api/auth/refresh` para obter um novo par de tokens sem logar novamente. Vida longa, armazenado no servidor (Redis) para permitir revogação.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "error.auth.invalid-credentials",
                                    summary = "Credenciais inválidas",
                                    value = """
                                            {
                                              "status": 401,
                                              "type": "about:blank",
                                              "title": "error.auth.invalid-credentials",
                                              "detail": "Invalid email or password",
                                              "instance": "/api/auth/login",
                                              "code": "error.auth.invalid-credentials"
                                            }
                                            """))),
            @ApiResponse(responseCode = "400", description = "Um ou mais campos são inválidos (e-mail ou senha ausentes)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "error.validation.failed",
                                    summary = "Campos inválidos",
                                    value = """
                                            {
                                              "status": 400,
                                              "type": "about:blank",
                                              "title": "error.validation.failed",
                                              "detail": "One or more fields are invalid",
                                              "instance": "/api/auth/login",
                                              "code": "error.validation.failed",
                                              "errors": [
                                                {
                                                  "field": "password",
                                                  "code": "error.validation.password.NotBlank",
                                                  "message": "Password is required"
                                                }
                                              ]
                                            }
                                            """)))
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(
            summary = "Renovar tokens de acesso",
            description = """
                    Gera um novo par de tokens (`access_token` + `refresh_token`) a partir de um refresh token válido.
                    
                    - O refresh token informado é **revogado** assim que o novo par é emitido com sucesso (rotação) — ele não pode ser reutilizado.
                    - Reutilizar um refresh token já rotacionado ou já revogado (via logout) resulta em `error.auth.refresh-revoked`.
                    - Um refresh token não pode ser usado como `access_token` em rotas protegidas.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Novo par de tokens gerado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token expirado, inválido ou revogado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = {
                                    @ExampleObject(
                                            name = "error.auth.refresh-expired",
                                            summary = "Refresh token expirado",
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "type": "about:blank",
                                                      "title": "error.auth.refresh-expired",
                                                      "detail": "Refresh token expired",
                                                      "instance": "/api/auth/refresh",
                                                      "code": "error.auth.refresh-expired"
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "error.auth.refresh-invalid",
                                            summary = "Refresh token inválido",
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "type": "about:blank",
                                                      "title": "error.auth.refresh-invalid",
                                                      "detail": "Invalid refresh token",
                                                      "instance": "/api/auth/refresh",
                                                      "code": "error.auth.refresh-invalid"
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "error.auth.refresh-revoked",
                                            summary = "Refresh token revogado ou não encontrado",
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "type": "about:blank",
                                                      "title": "error.auth.refresh-revoked",
                                                      "detail": "Refresh token revoked or not found",
                                                      "instance": "/api/auth/refresh",
                                                      "code": "error.auth.refresh-revoked"
                                                    }
                                                    """)
                            })),
            @ApiResponse(responseCode = "400", description = "Campo refresh_token ausente ou vazio",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "error.validation.failed",
                                    summary = "Campo obrigatório ausente",
                                    value = """
                                            {
                                              "status": 400,
                                              "type": "about:blank",
                                              "title": "error.validation.failed",
                                              "detail": "One or more fields are invalid",
                                              "instance": "/api/auth/refresh",
                                              "code": "error.validation.failed",
                                              "errors": [
                                                {
                                                  "field": "refresh_token",
                                                  "code": "error.validation.refresh_token.NotBlank",
                                                  "message": "Refresh token is required"
                                                }
                                              ]
                                            }
                                            """)))
    })
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    @Operation(
            summary = "Encerrar sessão (revogar refresh token)",
            description = """
                    Remove o refresh token informado do armazenamento no servidor (Redis), encerrando a sessão associada a ele.
                    
                    - Idempotente: se o token já não existir (já revogado, expirado ou já usado), a operação ainda retorna sucesso.
                    - Não invalida o `access_token` já emitido — ele continua válido até expirar naturalmente.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout realizado, nenhum conteúdo retornado", content = @Content),
            @ApiResponse(responseCode = "400", description = "Campo refresh_token ausente ou vazio",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiProblem.class),
                            examples = @ExampleObject(
                                    name = "error.validation.failed",
                                    summary = "Campo obrigatório ausente",
                                    value = """
                                            {
                                              "status": 400,
                                              "type": "about:blank",
                                              "title": "error.validation.failed",
                                              "detail": "One or more fields are invalid",
                                              "instance": "/api/auth/logout",
                                              "code": "error.validation.failed",
                                              "errors": [
                                                {
                                                  "field": "refresh_token",
                                                  "code": "error.validation.refresh_token.NotBlank",
                                                  "message": "Refresh token is required"
                                                }
                                              ]
                                            }
                                            """)))
    })
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }
}
