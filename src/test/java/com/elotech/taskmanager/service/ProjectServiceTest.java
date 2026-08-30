package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.request.project.CreateProjectRequest;
import com.elotech.taskmanager.domain.dto.request.project.UpdateProjectRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.project.ProjectResponse;
import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.ProjectMember;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.projectmember.ProjectMemberRepository;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import com.elotech.taskmanager.repository.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProjectAccessPolicy projectAccessPolicy;
    @Mock
    private CacheInvalidationService cacheInvalidationService;

    @Mock
    private TaskRepository taskRepository;

    private ProjectService projectService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, projectMemberRepository, currentUserService, projectAccessPolicy, taskRepository, cacheInvalidationService);
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Maria")
                .email("maria@exemplo.com")
                .password("hash")
                .build();
    }

    @Test
    void create_shouldCreateProjectAndOwnerMembership() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(UUID.randomUUID());
            return project;
        });

        ProjectResponse response = projectService.create(new CreateProjectRequest("Plataforma interna", "Backlog"));

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectRepository).save(projectCaptor.capture());
        verify(projectMemberRepository).save(memberCaptor.capture());

        assertThat(response.currentUserRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(projectCaptor.getValue().getOwnerId()).isEqualTo(currentUser.getId());
        assertThat(memberCaptor.getValue().getProjectId()).isEqualTo(response.id());
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo(currentUser.getId());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(MemberRole.ADMIN);
        verify(cacheInvalidationService).evictProjectMutationCaches();
    }

    @Test
    void list_shouldReturnProjectsForCurrentUserWithFixedPagination() {
        Project project = project(UUID.randomUUID(), currentUser.getId());
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(project)));
        given(projectAccessPolicy.requireRole(project.getId(), currentUser.getId())).willReturn(MemberRole.MEMBER);

        PageResponse<ProjectResponse> response = projectService.list(" Plataforma ", " Backlog ", "name,asc", 0, 200);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectRepository).findAll(any(Specification.class), pageableCaptor.capture());

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).currentUserRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("name").isAscending()).isTrue();
    }

    @Test
    void get_shouldRequireMembership() {
        UUID projectId = UUID.randomUUID();
        Project project = project(projectId, currentUser.getId());
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.ADMIN);

        ProjectResponse response = projectService.get(projectId);

        assertThat(response.id()).isEqualTo(projectId);
        assertThat(response.currentUserRole()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    void patch_shouldRequireAdminAndUpdateProvidedFields() {
        UUID projectId = UUID.randomUUID();
        Project project = project(projectId, currentUser.getId());
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireAdmin(projectId, currentUser.getId())).willReturn(project);

        ProjectResponse response = projectService.patch(projectId, new UpdateProjectRequest("Novo nome", null));

        assertThat(response.name()).isEqualTo("Novo nome");
        assertThat(project.getName()).isEqualTo("Novo nome");
        verify(projectAccessPolicy).requireAdmin(projectId, currentUser.getId());
    }

    @Test
    void patch_shouldRejectEmptyPayload() {
        UUID patchProjectId = UUID.randomUUID();
        UpdateProjectRequest emptyPatch = new UpdateProjectRequest(null, null);
        assertThatThrownBy(() -> projectService.patch(patchProjectId, emptyPatch))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("At least one field must be provided");
    }

    @Test
    void list_shouldRejectInvalidSortField() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);

        assertThatThrownBy(() -> projectService.list(null, null, "description,asc", 0, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Sort field is not allowed");
    }

    @Test
    void list_shouldRejectNegativePage() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);

        assertThatThrownBy(() -> projectService.list(null, null, null, -1, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Page must be greater than or equal to zero");
    }

    @Test
    void list_shouldRejectSizeLessThanOne() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);

        assertThatThrownBy(() -> projectService.list(null, null, null, 0, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Size must be greater than zero");
    }

    private Project project(UUID projectId, UUID ownerId) {
        return Project.builder()
                .id(projectId)
                .name("Plataforma interna")
                .description("Backlog")
                .ownerId(ownerId)
                .createdAt(LocalDateTime.of(2026, 1, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 10, 9, 0))
                .build();
    }
}
