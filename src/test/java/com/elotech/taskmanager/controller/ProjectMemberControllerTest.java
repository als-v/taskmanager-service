package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.request.member.AddProjectMembersRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.member.ProjectMemberResponse;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.ConflictException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.domain.error.ForbiddenException;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.service.ProjectMemberService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProjectMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectMemberService projectMemberService;

    @Test
    void listMembersPassesFiltersIgnoresRoleAndSerializesSnakeCase() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMemberResponse member = new ProjectMemberResponse(
                userId,
                "Ana Silva",
                "ana@example.com",
                MemberRole.MEMBER,
                LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        given(projectMemberService.list(projectId, 0, 20, "Ana", "example"))
                .willReturn(new PageResponse<>(List.of(member), 0, 20, 1, 1));

        mockMvc.perform(get("/api/projects/{projectId}/members", projectId)
                        .param("page", "0")
                        .param("size", "20")
                        .param("name", "Ana")
                        .param("email", "example")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].user_id").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].role").value("MEMBER"))
                .andExpect(jsonPath("$.content[0].joined_at").value("2026-01-10T09:00:00"))
                .andExpect(jsonPath("$.total_elements").value(1));

        verify(projectMemberService).list(projectId, 0, 20, "Ana", "example");
    }

    @Test
    void addMembersAcceptsOnlyUserIdsAndReturnsMembersAsMember() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMemberResponse member = new ProjectMemberResponse(userId, "Ana", "ana@example.com", MemberRole.MEMBER, LocalDateTime.of(2026, 1, 10, 9, 0));
        given(projectMemberService.add(eq(projectId), any(AddProjectMembersRequest.class))).willReturn(List.of(member));

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"user_ids":["%s"],"role":"ADMIN"}
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].user_id").value(userId.toString()))
                .andExpect(jsonPath("$[0].role").value("MEMBER"));

        verify(projectMemberService).add(eq(projectId), any(AddProjectMembersRequest.class));
    }

    @Test
    void addMembersReturnsMappedErrors() throws Exception {
        UUID projectId = UUID.randomUUID();
        given(projectMemberService.add(eq(projectId), any(AddProjectMembersRequest.class)))
                .willThrow(new ConflictException(ErrorMessages.PROJECT_MEMBER_ALREADY_EXISTS_CODE, ErrorMessages.PROJECT_MEMBER_ALREADY_EXISTS_MESSAGE));

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddProjectMembersRequest(List.of(UUID.randomUUID())))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorMessages.PROJECT_MEMBER_ALREADY_EXISTS_CODE));
    }

    @Test
    void addMembersCoversBadRequestForbiddenAndNotFoundContracts() throws Exception {
        UUID projectId = UUID.randomUUID();
        AddProjectMembersRequest request = new AddProjectMembersRequest(List.of(UUID.randomUUID()));

        given(projectMemberService.add(eq(projectId), any(AddProjectMembersRequest.class)))
                .willThrow(new BadRequestException(ErrorMessages.PROJECT_MEMBER_EMPTY_LIST_CODE, ErrorMessages.PROJECT_MEMBER_EMPTY_LIST_MESSAGE))
                .willThrow(new ForbiddenException(ErrorMessages.PROJECT_ADMIN_REQUIRED_CODE, ErrorMessages.PROJECT_ADMIN_REQUIRED_MESSAGE))
                .willThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND_CODE, ErrorMessages.USER_NOT_FOUND_MESSAGE));

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
