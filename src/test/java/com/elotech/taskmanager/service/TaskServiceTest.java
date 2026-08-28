package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.request.task.CreateTaskRequest;
import com.elotech.taskmanager.domain.dto.request.task.UpdateTaskRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.task.TaskResponse;
import com.elotech.taskmanager.domain.entity.Notification;
import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.entity.TaskLog;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.entity.UserNotification;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import com.elotech.taskmanager.domain.enumeration.NotificationType;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.BusinessException;
import com.elotech.taskmanager.domain.error.ForbiddenException;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.NotificationRepository;
import com.elotech.taskmanager.repository.ProjectMemberRepository;
import com.elotech.taskmanager.repository.TaskLogRepository;
import com.elotech.taskmanager.repository.TaskRepository;
import com.elotech.taskmanager.repository.UserNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskLogRepository taskLogRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserNotificationRepository userNotificationRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ProjectAccessPolicy projectAccessPolicy;

    private TaskService taskService;
    private User currentUser;
    private UUID projectId;
    private UUID assigneeId;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository,
                taskLogRepository,
                notificationRepository,
                userNotificationRepository,
                projectMemberRepository,
                currentUserService,
                projectAccessPolicy
        );
        currentUser = User.builder().id(UUID.randomUUID()).name("Maria").email("maria@example.com").build();
        projectId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
    }

    @Test
    void createsTaskWithoutAssignee() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(UUID.randomUUID());
            return task;
        });

        TaskResponse response = taskService.create(projectId, new CreateTaskRequest(
                "Criar login", "JWT", TaskStatus.TODO, Priority.HIGH, null, null));

        assertThat(response.projectId()).isEqualTo(projectId);
        assertThat(response.assigneeId()).isNull();
        verify(taskLogRepository).save(any(TaskLog.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void createsTaskWithMemberAssigneeAndGeneratesNotificationAndAudit() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.ADMIN);
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).willReturn(true);
        given(taskRepository.save(any(Task.class))).willAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(UUID.randomUUID());
            return task;
        });
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            return notification;
        });

        TaskResponse response = taskService.create(projectId, new CreateTaskRequest(
                "Criar login", "JWT", TaskStatus.TODO, Priority.HIGH, assigneeId, null));

        assertThat(response.assigneeId()).isEqualTo(assigneeId);
        ArgumentCaptor<TaskLog> logCaptor = ArgumentCaptor.forClass(TaskLog.class);
        verify(taskLogRepository, org.mockito.Mockito.times(2)).save(logCaptor.capture());
        assertThat(logCaptor.getAllValues()).extracting(TaskLog::getAction)
                .containsExactly(AuditAction.TASK_CREATED, AuditAction.ASSIGNEE_CHANGED);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        verify(userNotificationRepository).save(any(UserNotification.class));
    }

    @Test
    void rejectsAssigneeThatIsNotProjectMember() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).willReturn(false);

        CreateTaskRequest request = new CreateTaskRequest(
                "Criar login", null, TaskStatus.TODO, Priority.HIGH, assigneeId, null);

        assertThatThrownBy(() -> taskService.create(projectId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsDoneToTodo() {
        Task task = task(TaskStatus.DONE, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.ADMIN);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).willReturn(true);

        UUID taskId = task.getId();
        UpdateTaskRequest request = new UpdateTaskRequest(
                null, null, TaskStatus.TODO, null, null, null);

        assertThatThrownBy(() -> taskService.patch(projectId, taskId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsCriticalDoneByMember() {
        Task task = task(TaskStatus.IN_PROGRESS, Priority.CRITICAL, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).willReturn(true);

        UUID taskId = task.getId();
        UpdateTaskRequest request = new UpdateTaskRequest(
                null, null, TaskStatus.DONE, null, null, null);

        assertThatThrownBy(() -> taskService.patch(projectId, taskId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsSixthInProgressForSameAssigneeInProject() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.ADMIN);
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).willReturn(true);
        given(taskRepository.countByProjectIdAndUserIdAndStatus(projectId, assigneeId, TaskStatus.IN_PROGRESS)).willReturn(5L);

        CreateTaskRequest request = new CreateTaskRequest(
                "Criar login", null, TaskStatus.IN_PROGRESS, Priority.HIGH, assigneeId, null);

        assertThatThrownBy(() -> taskService.create(projectId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void allowsPartialUpdateWithValidFields() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).willReturn(true);

        TaskResponse response = taskService.patch(projectId, task.getId(), new UpdateTaskRequest(
                "Novo título", null, TaskStatus.IN_PROGRESS, null, null, null));

        assertThat(response.title()).isEqualTo("Novo título");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskLogRepository, org.mockito.Mockito.times(2)).save(any(TaskLog.class));
    }

    @Test
    void rejectsEmptyPatch() {
        UUID taskId = UUID.randomUUID();
        UpdateTaskRequest request = new UpdateTaskRequest(
                null, null, null, null, null, null);

        assertThatThrownBy(() -> taskService.patch(projectId, taskId, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deletesTaskForAdminAndRegistersAudit() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, null);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.ADMIN);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));

        taskService.delete(projectId, task.getId());

        ArgumentCaptor<TaskLog> logCaptor = ArgumentCaptor.forClass(TaskLog.class);
        verify(taskLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo(AuditAction.TASK_DELETED);
        verify(taskRepository).delete(task);
    }

    @Test
    void deniesDeleteForMember() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);

        UUID taskId = UUID.randomUUID();

        assertThatThrownBy(() -> taskService.delete(projectId, taskId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listsTasksWithDefaultPaginationAndSizeLimit() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, null);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(taskRepository.findAllByProjectId(eq(projectId), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(task)));

        PageResponse<TaskResponse> response = taskService.list(projectId, 0, 200);

        assertThat(response.content()).hasSize(1);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAllByProjectId(eq(projectId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    private Project project() {
        return Project.builder().id(projectId).ownerId(currentUser.getId()).name("Projeto").build();
    }

    private Task task(TaskStatus status, Priority priority, UUID assignee) {
        return Task.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .title("Criar login")
                .description("JWT")
                .status(status)
                .priority(priority)
                .userId(assignee)
                .dueDate(LocalDateTime.of(2026, 2, 15, 18, 0))
                .createdAt(LocalDateTime.of(2026, 1, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 10, 9, 0))
                .build();
    }
}
