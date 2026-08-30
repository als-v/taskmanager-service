package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.response.audit.AuditLogReferenceResponse;
import com.elotech.taskmanager.domain.dto.response.audit.AuditLogResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    void serializesAuditLogReferencesAsSnakeCase() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        AuditLogResponse log = auditLog(projectId, taskId, actorId);
        given(auditLogService.list(projectId, null, null, null, null, null, 0, 20))
                .willReturn(new PageResponse<>(List.of(log), 0, 20, 1, 1));

        mockMvc.perform(get("/api/projects/{projectId}/logs", projectId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(log.id().toString()))
                .andExpect(jsonPath("$.content[0].project_id").value(projectId.toString()))
                .andExpect(jsonPath("$.content[0].task.id").value(taskId.toString()))
                .andExpect(jsonPath("$.content[0].task.name").value("Criar endpoint de status"))
                .andExpect(jsonPath("$.content[0].actor.id").value(actorId.toString()))
                .andExpect(jsonPath("$.content[0].actor.name").value("Maria Silva"))
                .andExpect(jsonPath("$.content[0].task_id").doesNotExist())
                .andExpect(jsonPath("$.content[0].actor_id").doesNotExist())
                .andExpect(jsonPath("$.content[0].from_status").value("TODO"))
                .andExpect(jsonPath("$.content[0].to_status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.total_elements").value(1));
    }

    @Test
    void passesFiltersAndPaginationToService() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 1, 10, 8, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 10, 18, 0);
        given(auditLogService.list(projectId, taskId, actorId, AuditAction.STATUS_CHANGED, from, to, 1, 50))
                .willReturn(new PageResponse<>(List.of(), 1, 50, 0, 0));

        mockMvc.perform(get("/api/projects/{projectId}/logs", projectId)
                        .param("task_id", taskId.toString())
                        .param("actor_id", actorId.toString())
                        .param("action", "STATUS_CHANGED")
                        .param("created_at_from", "2026-01-10T08:00:00")
                        .param("created_at_to", "2026-01-10T18:00:00")
                        .param("page", "1")
                        .param("size", "50"))
                .andExpect(status().isOk());

        verify(auditLogService).list(projectId, taskId, actorId, AuditAction.STATUS_CHANGED, from, to, 1, 50);
    }

    @Test
    void returnsBadRequestWhenDateParameterIsInvalid() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(get("/api/projects/{projectId}/logs", projectId)
                        .param("created_at_from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.request.parameter-invalid"));
    }

    @Test
    void returnsBadRequestWhenCreatedAtRangeIsInvalid() throws Exception {
        UUID projectId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 1, 11, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 10, 0, 0);
        given(auditLogService.list(projectId, null, null, null, from, to, null, null))
                .willThrow(new BadRequestException(
                        ErrorMessages.REQUEST_PARAMETER_INVALID_CODE,
                        "created_at_from must be before or equal to created_at_to"
                ));

        mockMvc.perform(get("/api/projects/{projectId}/logs", projectId)
                        .param("created_at_from", "2026-01-11T00:00:00")
                        .param("created_at_to", "2026-01-10T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.request.parameter-invalid"));
    }

    private AuditLogResponse auditLog(UUID projectId, UUID taskId, UUID actorId) {
        return new AuditLogResponse(
                UUID.randomUUID(),
                projectId,
                new AuditLogReferenceResponse(taskId, "Criar endpoint de status"),
                new AuditLogReferenceResponse(actorId, "Maria Silva"),
                AuditAction.STATUS_CHANGED,
                TaskStatus.TODO,
                TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 1, 10, 9, 0)
        );
    }
}
