package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.criteria.TaskListCriteria;

import com.elotech.taskmanager.domain.dto.request.task.CreateTaskRequest;
import com.elotech.taskmanager.domain.dto.request.task.UpdateTaskAssigneeRequest;
import com.elotech.taskmanager.domain.dto.request.task.UpdateTaskDueDateRequest;
import com.elotech.taskmanager.domain.dto.request.task.UpdateTaskRequest;
import com.elotech.taskmanager.domain.dto.request.task.UpdateTaskStatusRequest;
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
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.notification.NotificationRepository;
import com.elotech.taskmanager.repository.projectmember.ProjectMemberRepository;
import com.elotech.taskmanager.repository.tasklog.TaskLogRepository;
import com.elotech.taskmanager.repository.task.TaskRepository;
import com.elotech.taskmanager.repository.usernotification.UserNotificationRepository;
import com.elotech.taskmanager.repository.user.UserRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ProjectAccessPolicy projectAccessPolicy;

    private TaskService taskService;
    private User currentUser;
    private UUID projectId;
    private UUID assigneeId;
    private User assigneeUser;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository,
                taskLogRepository,
                notificationRepository,
                userNotificationRepository,
                projectMemberRepository,
                userRepository,
                currentUserService,
                projectAccessPolicy
        );
        currentUser = User.builder().id(UUID.randomUUID()).name("Maria").email("maria@example.com").build();
        projectId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
        assigneeUser = User.builder().id(assigneeId).name("Maria Silva").email("maria.silva@example.com").build();
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
        assertThat(response.assignee()).isNull();
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
        given(userRepository.findById(assigneeId)).willReturn(Optional.of(assigneeUser));

        TaskResponse response = taskService.create(projectId, new CreateTaskRequest(
                "Criar login", "JWT", TaskStatus.TODO, Priority.HIGH, assigneeId, null));

        assertThat(response.assignee().id()).isEqualTo(assigneeId);
        assertThat(response.assignee().name()).isEqualTo(assigneeUser.getName());
        assertThat(response.assignee().email()).isEqualTo(assigneeUser.getEmail());
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
        given(taskRepository.countByProjectIdAndUserIdAndStatusAndDeletedAtIsNull(projectId, assigneeId, TaskStatus.IN_PROGRESS)).willReturn(5L);

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
        given(userRepository.findById(assigneeId)).willReturn(Optional.of(assigneeUser));

        TaskResponse response = taskService.patch(projectId, task.getId(), new UpdateTaskRequest(
                "Novo título", null, TaskStatus.IN_PROGRESS, null, null, null));

        assertThat(response.title()).isEqualTo("Novo título");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.assignee().id()).isEqualTo(assigneeId);
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
    void patchStatusUpdatesStatusAndGeneratesAudit() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(userRepository.findById(assigneeId)).willReturn(Optional.of(assigneeUser));

        TaskResponse response = taskService.patchStatus(projectId, task.getId(),
                new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS));

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        ArgumentCaptor<TaskLog> logCaptor = ArgumentCaptor.forClass(TaskLog.class);
        verify(taskLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo(AuditAction.STATUS_CHANGED);
        assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(logCaptor.getValue().getToStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void patchStatusRejectsDoneToTodo() {
        Task task = task(TaskStatus.DONE, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.ADMIN);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));

        UUID taskId = task.getId();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.TODO);

        assertThatThrownBy(() -> taskService.patchStatus(projectId, taskId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void patchStatusRejectsCriticalDoneByMember() {
        Task task = task(TaskStatus.IN_PROGRESS, Priority.CRITICAL, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));

        UUID taskId = task.getId();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);

        assertThatThrownBy(() -> taskService.patchStatus(projectId, taskId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void patchStatusRejectsWhenWipLimitExceeded() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.ADMIN);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(taskRepository.countByProjectIdAndUserIdAndStatusAndDeletedAtIsNull(projectId, assigneeId, TaskStatus.IN_PROGRESS))
                .willReturn(5L);

        UUID taskId = task.getId();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

        assertThatThrownBy(() -> taskService.patchStatus(projectId, taskId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void patchStatusIsNoopAuditWhenStatusUnchanged() {
        Task task = task(TaskStatus.IN_PROGRESS, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(userRepository.findById(assigneeId)).willReturn(Optional.of(assigneeUser));

        TaskResponse response = taskService.patchStatus(projectId, task.getId(),
                new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS));

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskLogRepository, never()).save(any(TaskLog.class));
    }

    @Test
    void patchAssigneeUpdatesAssigneeGeneratesAuditAndNotification() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, null);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.ADMIN);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).willReturn(true);
        given(userRepository.findById(assigneeId)).willReturn(Optional.of(assigneeUser));
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            return notification;
        });

        TaskResponse response = taskService.patchAssignee(projectId, task.getId(),
                new UpdateTaskAssigneeRequest(assigneeId));

        assertThat(response.assignee().id()).isEqualTo(assigneeId);
        ArgumentCaptor<TaskLog> logCaptor = ArgumentCaptor.forClass(TaskLog.class);
        verify(taskLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo(AuditAction.ASSIGNEE_CHANGED);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        verify(userNotificationRepository).save(any(UserNotification.class));
    }

    @Test
    void patchAssigneeToNullRemovesAssigneeAndSkipsNotification() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));

        TaskResponse response = taskService.patchAssignee(projectId, task.getId(),
                new UpdateTaskAssigneeRequest(null));

        assertThat(response.assignee()).isNull();
        ArgumentCaptor<TaskLog> logCaptor = ArgumentCaptor.forClass(TaskLog.class);
        verify(taskLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo(AuditAction.ASSIGNEE_CHANGED);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void patchAssigneeRejectsWhenNotProjectMember() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, null);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).willReturn(false);

        UUID taskId = task.getId();
        UpdateTaskAssigneeRequest request = new UpdateTaskAssigneeRequest(assigneeId);

        assertThatThrownBy(() -> taskService.patchAssignee(projectId, taskId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void patchAssigneeRejectsWhenWipLimitExceededForNewAssignee() {
        UUID otherAssigneeId = UUID.randomUUID();
        Task task = task(TaskStatus.IN_PROGRESS, Priority.HIGH, otherAssigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.ADMIN);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).willReturn(true);
        given(taskRepository.countByProjectIdAndUserIdAndStatusAndDeletedAtIsNull(projectId, assigneeId, TaskStatus.IN_PROGRESS))
                .willReturn(5L);

        UUID taskId = task.getId();
        UpdateTaskAssigneeRequest request = new UpdateTaskAssigneeRequest(assigneeId);

        assertThatThrownBy(() -> taskService.patchAssignee(projectId, taskId, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void patchAssigneeIsNoopAuditWhenAssigneeUnchanged() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).willReturn(true);
        given(userRepository.findById(assigneeId)).willReturn(Optional.of(assigneeUser));

        TaskResponse response = taskService.patchAssignee(projectId, task.getId(),
                new UpdateTaskAssigneeRequest(assigneeId));

        assertThat(response.assignee().id()).isEqualTo(assigneeId);
        verify(taskLogRepository, never()).save(any(TaskLog.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void patchDueDateChangesDueDateAndRegistersAudit() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, null);
        LocalDateTime newDueDate = LocalDateTime.of(2026, 2, 20, 18, 0);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));

        TaskResponse response = taskService.patchDueDate(projectId, task.getId(), new UpdateTaskDueDateRequest(newDueDate));

        assertThat(response.dueDate()).isEqualTo(newDueDate);
        assertThat(task.getDueDate()).isEqualTo(newDueDate);
        ArgumentCaptor<TaskLog> logCaptor = ArgumentCaptor.forClass(TaskLog.class);
        verify(taskLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo(AuditAction.DUE_DATE_CHANGED);
    }

    @Test
    void patchDueDateRemovesDueDateAndRegistersAudit() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, null);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));

        TaskResponse response = taskService.patchDueDate(projectId, task.getId(), new UpdateTaskDueDateRequest(null));

        assertThat(response.dueDate()).isNull();
        assertThat(task.getDueDate()).isNull();
        ArgumentCaptor<TaskLog> logCaptor = ArgumentCaptor.forClass(TaskLog.class);
        verify(taskLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo(AuditAction.DUE_DATE_CHANGED);
    }

    @Test
    void patchDueDateDoesNotRegisterAuditWhenUnchanged() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, null);
        LocalDateTime sameDueDate = task.getDueDate();
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.MEMBER);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));

        TaskResponse response = taskService.patchDueDate(projectId, task.getId(), new UpdateTaskDueDateRequest(sameDueDate));

        assertThat(response.dueDate()).isEqualTo(sameDueDate);
        verify(taskLogRepository, never()).save(any(TaskLog.class));
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
        assertThat(task.getDeletedAt()).isNotNull();
    }

    @Test
    void delete_shouldReturnNotFoundForAlreadyDeletedTask() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, assigneeId);
        task.setDeletedAt(LocalDateTime.now());
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(projectAccessPolicy.requireRole(projectId, currentUser.getId())).willReturn(MemberRole.ADMIN);
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));

        UUID taskId = task.getId();

        assertThatThrownBy(() -> taskService.delete(projectId, taskId))
                .isInstanceOf(NotFoundException.class);
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
        given(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(task)));

        PageResponse<TaskResponse> response = taskService.list(projectId, new TaskListCriteria(null, null, null, null, null, null, null, null, 0, 200, null));

        assertThat(response.content()).hasSize(1);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void listsTasksWithFiltersAndAllowedSort() {
        Task task = task(TaskStatus.IN_PROGRESS, Priority.CRITICAL, assigneeId);
        LocalDateTime dueDateFrom = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime dueDateTo = LocalDateTime.of(2026, 2, 28, 23, 59);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(task)));

        taskService.list(projectId, new TaskListCriteria(
                TaskStatus.IN_PROGRESS,
                Priority.CRITICAL,
                assigneeId,
                dueDateFrom,
                dueDateTo,
                " login ",
                " jwt ",
                "due_date,asc",
                1,
                50,
                null
        ));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("dueDate").isAscending()).isTrue();
    }

    @Test
    void acceptsAllowedTaskListSorts() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, null);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(task)));

        taskService.list(projectId, new TaskListCriteria(null, null, null, null, null, null, null, "priority,desc", 0, 20, null));
        taskService.list(projectId, new TaskListCriteria(null, null, null, null, null, null, null, "status,asc", 0, 20, null));

        verify(taskRepository, org.mockito.Mockito.times(2)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void rejectsTaskListSortFieldOutsideWhitelist() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());

        TaskListCriteria criteria = new TaskListCriteria(null, null, null, null, null, null, null, "title,asc", 0, 20, null);
        assertThatThrownBy(() -> taskService.list(projectId, criteria))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsTaskListSortDirectionOutsideWhitelist() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());

        TaskListCriteria criteria = new TaskListCriteria(null, null, null, null, null, null, null, "status,up", 0, 20, null);
        assertThatThrownBy(() -> taskService.list(projectId, criteria))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsTaskListInvertedDueDateRange() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());

        TaskListCriteria criteria = new TaskListCriteria(
                null, null, null, LocalDateTime.of(2026, 3, 1, 0, 0), LocalDateTime.of(2026, 2, 1, 0, 0), null, null, null, 0, 20, null
        );
        assertThatThrownBy(() -> taskService.list(projectId, criteria))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getReturnsAssigneeWhenTaskHasResponsible() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(userRepository.findById(assigneeId)).willReturn(Optional.of(assigneeUser));

        TaskResponse response = taskService.get(projectId, task.getId());

        assertThat(response.assignee().id()).isEqualTo(assigneeId);
        assertThat(response.assignee().name()).isEqualTo(assigneeUser.getName());
        assertThat(response.assignee().email()).isEqualTo(assigneeUser.getEmail());
    }

    @Test
    void getReturnsNullAssigneeWhenTaskHasNoResponsible() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, null);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));

        TaskResponse response = taskService.get(projectId, task.getId());

        assertThat(response.assignee()).isNull();
    }

    @Test
    void getReturnsNullAssigneeWhenAssigneeUserNoLongerExists() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(taskRepository.findById(task.getId())).willReturn(Optional.of(task));
        given(userRepository.findById(assigneeId)).willReturn(Optional.empty());

        TaskResponse response = taskService.get(projectId, task.getId());

        assertThat(response.assignee()).isNull();
    }

    @Test
    void listResolvesAssigneesInBatchForEachTask() {
        UUID otherAssigneeId = UUID.randomUUID();
        User otherAssigneeUser = User.builder().id(otherAssigneeId).name("João").email("joao@example.com").build();
        Task assignedTask = task(TaskStatus.TODO, Priority.HIGH, assigneeId);
        Task otherAssignedTask = task(TaskStatus.TODO, Priority.HIGH, otherAssigneeId);
        Task unassignedTask = task(TaskStatus.TODO, Priority.HIGH, null);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(assignedTask, otherAssignedTask, unassignedTask)));
        given(userRepository.findAllById(any())).willReturn(List.of(assigneeUser, otherAssigneeUser));

        PageResponse<TaskResponse> response = taskService.list(projectId,
                new TaskListCriteria(null, null, null, null, null, null, null, null, 0, 200, null));

        assertThat(response.content()).extracting(TaskResponse::id, r -> r.assignee() == null ? null : r.assignee().id())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(assignedTask.getId(), assigneeId),
                        org.assertj.core.groups.Tuple.tuple(otherAssignedTask.getId(), otherAssigneeId),
                        org.assertj.core.groups.Tuple.tuple(unassignedTask.getId(), null)
                );
    }

    @Test
    void listReturnsNullAssigneeWhenTaskUserIdIsInconsistent() {
        Task task = task(TaskStatus.TODO, Priority.HIGH, assigneeId);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project());
        given(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(task)));
        given(userRepository.findAllById(any())).willReturn(List.of());

        PageResponse<TaskResponse> response = taskService.list(projectId,
                new TaskListCriteria(null, null, null, null, null, null, null, null, 0, 200, null));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).assignee()).isNull();
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
