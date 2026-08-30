package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardProjectResponse;
import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardResponse;
import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardWipAssigneeResponse;
import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardWipResponse;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void getDashboardReturnsResponse() throws Exception {
        UUID projectId = UUID.randomUUID();
        given(dashboardService.getDashboard(null)).willReturn(response(null, projectId));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects_total").value(1))
                .andExpect(jsonPath("$.tasks_total").value(3))
                .andExpect(jsonPath("$.by_status.TODO").value(1))
                .andExpect(jsonPath("$.by_status.IN_PROGRESS").value(2))
                .andExpect(jsonPath("$.by_status.DONE").value(0))
                .andExpect(jsonPath("$.by_priority.LOW").value(0))
                .andExpect(jsonPath("$.by_priority.MEDIUM").value(1))
                .andExpect(jsonPath("$.by_priority.HIGH").value(2))
                .andExpect(jsonPath("$.by_priority.CRITICAL").value(0))
                .andExpect(jsonPath("$.overdue").value(4))
                .andExpect(jsonPath("$.due_soon").value(7))
                .andExpect(jsonPath("$.wip_by_assignee").doesNotExist())
                .andExpect(jsonPath("$.projects[0].id").value(projectId.toString()))
                .andExpect(jsonPath("$.projects[0].name").value("API"))
                .andExpect(jsonPath("$.selected_project_id").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.generated_at").value("2026-08-29T10:00:00"));
    }

    @Test
    void getDashboardAcceptsProjectFilter() throws Exception {
        UUID projectId = UUID.randomUUID();
        given(dashboardService.getDashboard(projectId)).willReturn(response(projectId, projectId));

        mockMvc.perform(get("/api/dashboard").param("project_id", projectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected_project_id").value(projectId.toString()));

        verify(dashboardService).getDashboard(projectId);
    }

    @Test
    void getDashboardMapsInaccessibleProjectTo404() throws Exception {
        UUID projectId = UUID.randomUUID();
        given(dashboardService.getDashboard(projectId))
                .willThrow(new NotFoundException(ErrorMessages.PROJECT_NOT_FOUND_CODE, ErrorMessages.PROJECT_NOT_FOUND_MESSAGE));

        mockMvc.perform(get("/api/dashboard").param("project_id", projectId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorMessages.PROJECT_NOT_FOUND_CODE));
    }

    @Test
    void getWipReturnsResponse() throws Exception {
        UUID assigneeId = UUID.randomUUID();
        given(dashboardService.getWip(null)).willReturn(new DashboardWipResponse(
                List.of(new DashboardWipAssigneeResponse(assigneeId, "Maria Silva", "maria@example.com", 3)),
                null,
                LocalDateTime.of(2026, 8, 29, 10, 0)
        ));

        mockMvc.perform(get("/api/dashboard/wip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].user_id").value(assigneeId.toString()))
                .andExpect(jsonPath("$.items[0].name").value("Maria Silva"))
                .andExpect(jsonPath("$.items[0].email").value("maria@example.com"))
                .andExpect(jsonPath("$.items[0].in_progress").value(3))
                .andExpect(jsonPath("$.selected_project_id").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.generated_at").value("2026-08-29T10:00:00"));
    }

    @Test
    void getWipAcceptsProjectFilter() throws Exception {
        UUID projectId = UUID.randomUUID();
        given(dashboardService.getWip(projectId)).willReturn(new DashboardWipResponse(List.of(), projectId, LocalDateTime.now()));

        mockMvc.perform(get("/api/dashboard/wip").param("project_id", projectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected_project_id").value(projectId.toString()));

        verify(dashboardService).getWip(projectId);
    }

    @Test
    void getWipMapsInaccessibleProjectTo404() throws Exception {
        UUID projectId = UUID.randomUUID();
        given(dashboardService.getWip(projectId))
                .willThrow(new NotFoundException(ErrorMessages.PROJECT_NOT_FOUND_CODE, ErrorMessages.PROJECT_NOT_FOUND_MESSAGE));

        mockMvc.perform(get("/api/dashboard/wip").param("project_id", projectId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorMessages.PROJECT_NOT_FOUND_CODE));
    }

    private DashboardResponse response(UUID selectedProjectId, UUID projectId) {
        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        byStatus.put(TaskStatus.TODO, 1L);
        byStatus.put(TaskStatus.IN_PROGRESS, 2L);
        byStatus.put(TaskStatus.DONE, 0L);

        Map<Priority, Long> byPriority = new EnumMap<>(Priority.class);
        byPriority.put(Priority.LOW, 0L);
        byPriority.put(Priority.MEDIUM, 1L);
        byPriority.put(Priority.HIGH, 2L);
        byPriority.put(Priority.CRITICAL, 0L);

        return new DashboardResponse(
                1,
                3,
                byStatus,
                byPriority,
                4,
                7,
                List.of(new DashboardProjectResponse(projectId, "API")),
                selectedProjectId,
                LocalDateTime.of(2026, 8, 29, 10, 0)
        );
    }
}
