package com.elotech.taskmanager.web.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @Schema(description = "Refresh token emitido no login ou na renovação anterior", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
