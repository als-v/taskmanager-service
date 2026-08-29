package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.request.task.CreateTaskRequest;
import com.elotech.taskmanager.domain.dto.request.task.UpdateTaskRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.task.TaskAssigneeResponse;
import com.elotech.taskmanager.domain.dto.response.task.TaskResponse;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.domain.error.BusinessException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.domain.error.ForbiddenException;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.domain.error.UnauthorizedException;
import com.elotech.taskmanager.domain.criteria.TaskListCriteria;
import com.elotech.taskmanager.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    void listsTasks() throws Exception {
        UUID projectId = UUID.randomUUID();
        TaskResponse task = taskResponse(projectId, UUID.randomUUID(), UUID.randomUUID());
        given(taskService.list(eq(projectId), any(TaskListCriteria.class))).willReturn(new PageResponse<>(List.of(task), 0, 20, 1, 1));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                .param("status", "TODO")
                .param("priority", "HIGH")
                .param("assignee_id", task.assignee().id().toString())
                .param("due_date_from", "2026-02-01T00:00:00")
                .param("due_date_to", "2026-02-28T23:59:59")
                .param("title", "login")
                .param("description", "jwt")
                .param("sort", "due_date,asc")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(task.id().toString()))
                .andExpect(jsonPath("$.content[0].assignee.id").value(task.assignee().id().toString()))
                .andExpect(jsonPath("$.content[0].assignee.name").value(task.assignee().name()))
                .andExpect(jsonPath("$.content[0].assignee.email").value(task.assignee().email()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.total_pages").value(1));

        org.mockito.ArgumentCaptor<TaskListCriteria> criteriaCaptor = org.mockito.ArgumentCaptor.forClass(TaskListCriteria.class);
        verify(taskService).list(eq(projectId), criteriaCaptor.capture());
        TaskListCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.status()).isEqualTo(TaskStatus.TODO);
        assertThat(criteria.priority()).isEqualTo(Priority.HIGH);
        assertThat(criteria.assigneeId()).isEqualTo(task.assignee().id());
        assertThat(criteria.dueDateFrom()).isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
        assertThat(criteria.dueDateTo()).isEqualTo(LocalDateTime.of(2026, 2, 28, 23, 59, 59));
        assertThat(criteria.title()).isEqualTo("login");
        assertThat(criteria.description()).isEqualTo("jwt");
        assertThat(criteria.sort()).isEqualTo("due_date,asc");
        assertThat(criteria.page()).isZero();
        assertThat(criteria.size()).isEqualTo(20);
    }

    @Test
    void listsTasksWithUnassignedFilter() throws Exception {
        UUID projectId = UUID.randomUUID();
        TaskResponse task = taskResponse(projectId, UUID.randomUUID(), null);
        given(taskService.list(eq(projectId), any(TaskListCriteria.class))).willReturn(new PageResponse<>(List.of(task), 0, 20, 1, 1));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .param("unassigned", "true"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<TaskListCriteria> criteriaCaptor = org.mockito.ArgumentCaptor.forClass(TaskListCriteria.class);
        verify(taskService).list(eq(projectId), criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().unassigned()).isTrue();
    }

    @Test
    void getsTask() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        TaskResponse task = taskResponse(projectId, taskId, null);
        given(taskService.get(projectId, taskId)).willReturn(task);

        mockMvc.perform(get("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.project_id").value(projectId.toString()))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createsTask() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        TaskResponse task = taskResponse(projectId, UUID.randomUUID(), assigneeId);
        CreateTaskRequest request = new CreateTaskRequest("Criar login", "JWT", TaskStatus.TODO, Priority.HIGH, assigneeId, null);
        given(taskService.create(eq(projectId), any(CreateTaskRequest.class))).willReturn(task);

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(task.id().toString()));
    }

    @Test
    void rejectsInvalidCreatePayload() throws Exception {
        UUID projectId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest("", null, null, null, null, null);

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorMessages.VALIDATION_FAILED_CODE));
    }

    @Test
    void patchesTask() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UpdateTaskRequest request = new UpdateTaskRequest("Novo título", null, TaskStatus.IN_PROGRESS, null, null, null);
        TaskResponse task = taskResponse(projectId, taskId, null);
        given(taskService.patch(eq(projectId), eq(taskId), any(UpdateTaskRequest.class))).willReturn(task);

        mockMvc.perform(patch("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()));
    }

    @Test
    void mapsBusinessRuleViolationTo422() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UpdateTaskRequest request = new UpdateTaskRequest(null, null, TaskStatus.TODO, null, null, null);
        given(taskService.patch(eq(projectId), eq(taskId), any(UpdateTaskRequest.class)))
                .willThrow(new BusinessException(ErrorMessages.TASK_DONE_TO_TODO_CODE, ErrorMessages.TASK_DONE_TO_TODO_MESSAGE));

        mockMvc.perform(patch("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorMessages.TASK_DONE_TO_TODO_CODE));
    }

    @Test
    void mapsNotFoundTo404() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        given(taskService.get(projectId, taskId))
                .willThrow(new NotFoundException(ErrorMessages.TASK_NOT_FOUND_CODE, ErrorMessages.TASK_NOT_FOUND_MESSAGE));

        mockMvc.perform(get("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorMessages.TASK_NOT_FOUND_CODE));
    }

    @Test
    void mapsUnauthorizedTo401() throws Exception {
        UUID projectId = UUID.randomUUID();
        given(taskService.list(eq(projectId), any(TaskListCriteria.class)))
                .willThrow(new UnauthorizedException(ErrorMessages.AUTH_UNAUTHENTICATED_CODE, ErrorMessages.AUTH_UNAUTHENTICATED_MESSAGE));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorMessages.AUTH_UNAUTHENTICATED_CODE));
    }

    @Test
    void deletesTask() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        mockMvc.perform(delete("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId))
                .andExpect(status().isNoContent());

        verify(taskService).delete(projectId, taskId);
    }

    @Test
    void mapsDeleteForbiddenTo403() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ForbiddenException(
                        ErrorMessages.TASK_DELETE_ADMIN_REQUIRED_CODE,
                        ErrorMessages.TASK_DELETE_ADMIN_REQUIRED_MESSAGE
                ))
                .when(taskService).delete(projectId, taskId);

        mockMvc.perform(delete("/api/projects/{projectId}/tasks/{taskId}", projectId, taskId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorMessages.TASK_DELETE_ADMIN_REQUIRED_CODE));
    }

    private TaskResponse taskResponse(UUID projectId, UUID taskId, UUID assigneeId) {
        TaskAssigneeResponse assignee = assigneeId == null
                ? null
                : new TaskAssigneeResponse(assigneeId, "Maria Silva", "maria@example.com");

        return new TaskResponse(
                taskId,
                projectId,
                "Criar login",
                "JWT",
                TaskStatus.TODO,
                Priority.HIGH,
                assignee,
                LocalDateTime.of(2026, 2, 15, 18, 0),
                LocalDateTime.of(2026, 1, 10, 9, 0),
                LocalDateTime.of(2026, 1, 10, 9, 0)
        );
    }
}
