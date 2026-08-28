package com.elotech.taskmanager.domain.dto.response.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "Token de acesso (JWT), enviado no header Authorization como Bearer", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "Token de renovação (JWT), usado em /api/auth/refresh para obter um novo par de tokens", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,

        @Schema(description = "Tipo do token de acesso", example = "Bearer")
        String tokenType,

        @Schema(description = "Dados do usuário autenticado")
        UserResponse user
) {
}
