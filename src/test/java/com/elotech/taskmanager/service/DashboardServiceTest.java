package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardProjectResponse;
import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardResponse;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import com.elotech.taskmanager.repository.task.TaskPriorityCountProjection;
import com.elotech.taskmanager.repository.task.TaskRepository;
import com.elotech.taskmanager.repository.task.TaskStatusCountProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProjectAccessPolicy projectAccessPolicy;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    private DashboardService dashboardService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(currentUserService, projectAccessPolicy, projectRepository, taskRepository);
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Maria")
                .email("maria@example.com")
                .password("hash")
                .build();
    }

    @Test
    void getDashboardWithoutFilterSummarizesAllMemberProjects() {
        UUID projectA = UUID.randomUUID();
        UUID projectB = UUID.randomUUID();
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectRepository.findDashboardProjectsByMemberUserId(currentUser.getId())).willReturn(List.of(
                new DashboardProjectResponse(projectA, "API"),
                new DashboardProjectResponse(projectB, "Frontend")
        ));
        given(taskRepository.countByStatusForMemberProjects(currentUser.getId())).willReturn(List.of(
                new StatusCount(TaskStatus.TODO, 2),
                new StatusCount(TaskStatus.IN_PROGRESS, 3)
        ));
        given(taskRepository.countByPriorityForMemberProjects(currentUser.getId())).willReturn(List.of(
                new PriorityCount(Priority.HIGH, 4),
                new PriorityCount(Priority.CRITICAL, 1)
        ));

        DashboardResponse response = dashboardService.getDashboard(null);

        assertThat(response.projectsTotal()).isEqualTo(2);
        assertThat(response.tasksTotal()).isEqualTo(5);
        assertThat(response.byStatus()).containsEntry(TaskStatus.TODO, 2L)
                .containsEntry(TaskStatus.IN_PROGRESS, 3L)
                .containsEntry(TaskStatus.DONE, 0L);
        assertThat(response.byPriority()).containsEntry(Priority.LOW, 0L)
                .containsEntry(Priority.MEDIUM, 0L)
                .containsEntry(Priority.HIGH, 4L)
                .containsEntry(Priority.CRITICAL, 1L);
        assertThat(response.projects()).extracting(DashboardProjectResponse::id).containsExactly(projectA, projectB);
        assertThat(response.selectedProjectId()).isNull();
        assertThat(response.generatedAt()).isNotNull();
    }

    @Test
    void getDashboardWithFilterSummarizesOnlySelectedProject() {
        UUID projectId = UUID.randomUUID();
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectRepository.findDashboardProjectsByMemberUserId(currentUser.getId()))
                .willReturn(List.of(new DashboardProjectResponse(projectId, "API")));
        given(taskRepository.countByStatusForMemberProject(currentUser.getId(), projectId))
                .willReturn(List.of(new StatusCount(TaskStatus.DONE, 7)));
        given(taskRepository.countByPriorityForMemberProject(currentUser.getId(), projectId))
                .willReturn(List.of(new PriorityCount(Priority.MEDIUM, 7)));

        DashboardResponse response = dashboardService.getDashboard(projectId);

        verify(projectAccessPolicy).requireMember(projectId, currentUser.getId());
        assertThat(response.projectsTotal()).isEqualTo(1);
        assertThat(response.tasksTotal()).isEqualTo(7);
        assertThat(response.byStatus()).containsEntry(TaskStatus.DONE, 7L)
                .containsEntry(TaskStatus.TODO, 0L);
        assertThat(response.byPriority()).containsEntry(Priority.MEDIUM, 7L)
                .containsEntry(Priority.CRITICAL, 0L);
        assertThat(response.projects()).hasSize(1);
        assertThat(response.selectedProjectId()).isEqualTo(projectId);
    }

    @Test
    void getDashboardWithoutProjectsReturnsZeroedPayload() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectRepository.findDashboardProjectsByMemberUserId(currentUser.getId())).willReturn(List.of());
        given(taskRepository.countByStatusForMemberProjects(currentUser.getId())).willReturn(List.of());
        given(taskRepository.countByPriorityForMemberProjects(currentUser.getId())).willReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard(null);

        assertThat(response.projectsTotal()).isZero();
        assertThat(response.tasksTotal()).isZero();
        assertThat(response.projects()).isEmpty();
        assertThat(response.byStatus()).containsOnly(
                org.assertj.core.data.MapEntry.entry(TaskStatus.TODO, 0L),
                org.assertj.core.data.MapEntry.entry(TaskStatus.IN_PROGRESS, 0L),
                org.assertj.core.data.MapEntry.entry(TaskStatus.DONE, 0L)
        );
        assertThat(response.byPriority()).containsOnly(
                org.assertj.core.data.MapEntry.entry(Priority.LOW, 0L),
                org.assertj.core.data.MapEntry.entry(Priority.MEDIUM, 0L),
                org.assertj.core.data.MapEntry.entry(Priority.HIGH, 0L),
                org.assertj.core.data.MapEntry.entry(Priority.CRITICAL, 0L)
        );
    }

    private record StatusCount(TaskStatus status, long total) implements TaskStatusCountProjection {

        @Override
        public TaskStatus getStatus() {
            return status;
        }

        @Override
        public long getTotal() {
            return total;
        }
    }

    private record PriorityCount(Priority priority, long total) implements TaskPriorityCountProjection {

        @Override
        public Priority getPriority() {
            return priority;
        }

        @Override
        public long getTotal() {
            return total;
        }
    }
}
