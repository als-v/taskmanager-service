package com.elotech.taskmanager.domain.dto.response.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Envelope padrão para respostas paginadas da API")
public record PageResponse<T>(
        @Schema(description = "Itens da página atual")
        List<T> content,

        @Schema(description = "Página atual, começando em 0", example = "0")
        int page,

        @Schema(description = "Quantidade efetiva de itens por página", example = "20")
        int size,

        @Schema(description = "Total de registros encontrados", example = "42")
        long totalElements,

        @Schema(description = "Total de páginas disponíveis", example = "3")
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
