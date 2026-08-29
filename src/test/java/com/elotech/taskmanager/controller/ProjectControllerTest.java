package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.request.project.CreateProjectRequest;
import com.elotech.taskmanager.domain.dto.request.project.UpdateProjectRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.project.ProjectResponse;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.service.ProjectService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void list_shouldReturnPagedProjects() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ProjectResponse project = projectResponse(projectId, ownerId, MemberRole.ADMIN);

        given(projectService.list(0, 20)).willReturn(new PageResponse<>(List.of(project), 0, 20, 1, 1));

        mockMvc.perform(get("/api/projects")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(projectId.toString()))
                .andExpect(jsonPath("$.content[0].current_user_role").value("ADMIN"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.total_pages").value(1));
    }

    @Test
    void get_shouldReturnProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        given(projectService.get(projectId)).willReturn(projectResponse(projectId, ownerId, MemberRole.MEMBER));

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.owner_id").value(ownerId.toString()))
                .andExpect(jsonPath("$.current_user_role").value("MEMBER"));
    }

    @Test
    void create_shouldReturn201WithProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest("Plataforma interna", "Backlog");
        given(projectService.create(any(CreateProjectRequest.class)))
                .willReturn(projectResponse(projectId, ownerId, MemberRole.ADMIN));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("Plataforma interna"));
    }

    @Test
    void create_shouldReturn400WhenNameIsBlank() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("", "Backlog");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorMessages.VALIDATION_FAILED_CODE))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void patch_shouldUpdateProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UpdateProjectRequest request = new UpdateProjectRequest("Novo nome", null);
        given(projectService.patch(eq(projectId), any(UpdateProjectRequest.class)))
                .willReturn(projectResponse(projectId, ownerId, MemberRole.ADMIN));

        mockMvc.perform(patch("/api/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()));

        verify(projectService).patch(eq(projectId), any(UpdateProjectRequest.class));
    }

    @Test
    void delete_shouldNotBeMapped() throws Exception {
        mockMvc.perform(delete("/api/projects/{id}", UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed());
    }

    private ProjectResponse projectResponse(UUID projectId, UUID ownerId, MemberRole role) {
        return new ProjectResponse(
                projectId,
                "Plataforma interna",
                "Backlog",
                ownerId,
                role,
                LocalDateTime.of(2026, 1, 10, 9, 0),
                LocalDateTime.of(2026, 1, 10, 9, 0)
        );
    }
}
