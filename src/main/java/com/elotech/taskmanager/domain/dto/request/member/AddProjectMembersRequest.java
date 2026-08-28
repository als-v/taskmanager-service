package com.elotech.taskmanager.domain.dto.request.member;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record AddProjectMembersRequest(
        @Schema(
                description = "Usuários que serão adicionados ao projeto como MEMBER",
                example = "[\"0d949a6f-6dd3-49c5-b0ff-7e7f69dcb192\"]"
        )
        List<UUID> userIds
) {
}
