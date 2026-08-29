package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.response.audit.AuditLogResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
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

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuditLogService auditLogService;

    @Test
    void serializesAuditLogAsSnakeCase() throws Exception {
        UUID projectId = UUID.randomUUID();
        AuditLogResponse log = auditLog(projectId, UUID.randomUUID(), UUID.randomUUID());
        given(auditLogService.list(projectId, null, null, null, 0, 20))
                .willReturn(new PageResponse<>(List.of(log), 0, 20, 1, 1));

        mockMvc.perform(get("/api/projects/{projectId}/audit-log", projectId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(log.id().toString()))
                .andExpect(jsonPath("$.content[0].project_id").value(projectId.toString()))
                .andExpect(jsonPath("$.content[0].task_id").value(log.taskId().toString()))
                .andExpect(jsonPath("$.content[0].actor_id").value(log.actorId().toString()))
                .andExpect(jsonPath("$.content[0].from_status").value("TODO"))
                .andExpect(jsonPath("$.content[0].to_status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.total_elements").value(1));
    }

    @Test
    void passesQueryParamsToService() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        given(auditLogService.list(projectId, taskId, actorId, AuditAction.STATUS_CHANGED, 1, 50))
                .willReturn(new PageResponse<>(List.of(), 1, 50, 0, 0));

        mockMvc.perform(get("/api/projects/{projectId}/audit-log", projectId)
                        .param("task_id", taskId.toString())
                        .param("actor_id", actorId.toString())
                        .param("action", "STATUS_CHANGED")
                        .param("page", "1")
                        .param("size", "50"))
                .andExpect(status().isOk());

        verify(auditLogService).list(projectId, taskId, actorId, AuditAction.STATUS_CHANGED, 1, 50);
    }

    @Test
    void returnsBadRequestForInvalidUuid() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(get("/api/projects/{projectId}/audit-log", projectId)
                        .param("task_id", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.request.parameter-invalid"));
    }

    private AuditLogResponse auditLog(UUID projectId, UUID taskId, UUID actorId) {
        return new AuditLogResponse(
                UUID.randomUUID(),
                projectId,
                taskId,
                actorId,
                AuditAction.STATUS_CHANGED,
                TaskStatus.TODO,
                TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 1, 10, 9, 0)
        );
    }
}
