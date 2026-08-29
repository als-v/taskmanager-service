package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.criteria.TaskListCriteria;

import com.elotech.taskmanager.domain.dto.request.task.CreateTaskRequest;
import com.elotech.taskmanager.domain.dto.request.task.UpdateTaskRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.task.TaskResponse;
import com.elotech.taskmanager.domain.entity.Notification;
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
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.pagination.PageRequests;
import com.elotech.taskmanager.domain.error.ForbiddenException;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.notification.NotificationRepository;
import com.elotech.taskmanager.repository.projectmember.ProjectMemberRepository;
import com.elotech.taskmanager.repository.tasklog.TaskLogRepository;
import com.elotech.taskmanager.repository.task.TaskRepository;
import com.elotech.taskmanager.repository.task.TaskSpecifications;
import com.elotech.taskmanager.repository.usernotification.UserNotificationRepository;
import com.elotech.taskmanager.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private static final int WIP_LIMIT = 5;
    private static final Sort DEFAULT_LIST_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Map<String, String> LIST_SORT_FIELDS = Map.of(
            "created_at", "createdAt",
            "due_date", "dueDate",
            "priority", "priority",
            "status", "status"
    );

    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAccessPolicy projectAccessPolicy;

    public TaskService(
            TaskRepository taskRepository,
            TaskLogRepository taskLogRepository,
            NotificationRepository notificationRepository,
            UserNotificationRepository userNotificationRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ProjectAccessPolicy projectAccessPolicy
    ) {
        this.taskRepository = taskRepository;
        this.taskLogRepository = taskLogRepository;
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.projectAccessPolicy = projectAccessPolicy;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(UUID projectId, TaskListCriteria criteria) {
        User currentUser = currentUserService.getCurrentUser();
        projectAccessPolicy.requireMember(projectId, currentUser.getId());

        TaskListCriteria normalized = normalize(criteria);
        validateDueDateRange(normalized.dueDateFrom(), normalized.dueDateTo());

        Page<Task> page = taskRepository.findAll(
                TaskSpecifications.byCriteria(projectId, normalized),
                PageRequests.of(normalized.page(), normalized.size(), sort(normalized.sort()))
        );

        Map<UUID, User> assigneesById = findAssigneesByTasks(page.getContent());

        return PageResponse.from(page.map(task -> TaskResponse.from(task, assigneesById.get(task.getUserId()))));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID projectId, UUID taskId) {
        User currentUser = currentUserService.getCurrentUser();
        projectAccessPolicy.requireMember(projectId, currentUser.getId());

        Task task = requireTaskInProject(projectId, taskId);
        return TaskResponse.from(task, resolveAssignee(task.getUserId()));
    }

    @Transactional
    public TaskResponse create(UUID projectId, CreateTaskRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        MemberRole actorRole = projectAccessPolicy.requireRole(projectId, currentUser.getId());

        validateAssignee(projectId, request.assigneeId());
        validateCriticalDone(request.priority(), request.status(), actorRole);
        validateWipLimit(projectId, request.assigneeId(), request.status(), null);

        Task task = Task.builder()
                .projectId(projectId)
                .title(request.title())
                .description(request.description())
                .status(request.status())
                .priority(request.priority())
                .userId(request.assigneeId())
                .dueDate(request.dueDate())
                .build();

        Task saved = taskRepository.save(task);
        saveAudit(saved, currentUser.getId(), AuditAction.TASK_CREATED, null, null);

        if (saved.getUserId() != null) {
            saveAudit(saved, currentUser.getId(), AuditAction.ASSIGNEE_CHANGED, null, null);
            notifyAssignee(saved, currentUser.getId());
        }

        return TaskResponse.from(saved, resolveAssignee(saved.getUserId()));
    }

    @Transactional
    public TaskResponse patch(UUID projectId, UUID taskId, UpdateTaskRequest request) {
        validatePatchPayload(request);

        User currentUser = currentUserService.getCurrentUser();
        MemberRole actorRole = projectAccessPolicy.requireRole(projectId, currentUser.getId());
        Task task = requireTaskInProject(projectId, taskId);

        validatePatch(projectId, request, actorRole, task);
        applyPatch(task, request, currentUser.getId());

        return TaskResponse.from(task, resolveAssignee(task.getUserId()));
    }

    @Transactional
    public void delete(UUID projectId, UUID taskId) {
        User currentUser = currentUserService.getCurrentUser();
        projectAccessPolicy.requireMember(projectId, currentUser.getId());
        MemberRole role = projectAccessPolicy.requireRole(projectId, currentUser.getId());

        if (role != MemberRole.ADMIN) {
            throw new ForbiddenException(ErrorMessages.TASK_DELETE_ADMIN_REQUIRED_CODE, ErrorMessages.TASK_DELETE_ADMIN_REQUIRED_MESSAGE);
        }

        Task task = requireTaskInProject(projectId, taskId);
        saveAudit(task, currentUser.getId(), AuditAction.TASK_DELETED, null, null);
        task.setDeletedAt(LocalDateTime.now());
    }

    private void validatePatchPayload(UpdateTaskRequest request) {
        if (request.hasNoChanges()) {
            throw new BadRequestException(ErrorMessages.TASK_NO_FIELDS_CODE, ErrorMessages.TASK_NO_FIELDS_MESSAGE);
        }

        if (request.title() != null && request.title().isBlank()) {
            throw new BadRequestException(ErrorMessages.TASK_TITLE_BLANK_CODE, ErrorMessages.TASK_TITLE_BLANK_MESSAGE);
        }
    }


    private TaskListCriteria normalize(TaskListCriteria criteria) {
        if (criteria == null) return new TaskListCriteria(null, null, null, null, null, null, null, null, null, null, null);

        return new TaskListCriteria(
                criteria.status(),
                criteria.priority(),
                criteria.assigneeId(),
                criteria.dueDateFrom(),
                criteria.dueDateTo(),
                blankToNull(criteria.title()),
                blankToNull(criteria.description()),
                blankToNull(criteria.sort()),
                criteria.page(),
                criteria.size(),
                criteria.unassigned()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private void validateDueDateRange(LocalDateTime dueDateFrom, LocalDateTime dueDateTo) {
        if (dueDateFrom != null && dueDateTo != null && dueDateFrom.isAfter(dueDateTo)) {
            throw new BadRequestException(
                    ErrorMessages.REQUEST_PARAMETER_INVALID_CODE,
                    "due_date_from must be before or equal to due_date_to"
            );
        }
    }

    private Sort sort(String rawSort) {
        if (rawSort == null) return DEFAULT_LIST_SORT;
        String[] parts = rawSort.split(",");

        if (parts.length != 2) {
            throw new BadRequestException(
                    ErrorMessages.REQUEST_PARAMETER_INVALID_CODE,
                    "Sort must use field,direction format"
            );
        }

        String property = LIST_SORT_FIELDS.get(parts[0].trim());

        if (property == null) {
            throw new BadRequestException(
                    ErrorMessages.REQUEST_PARAMETER_INVALID_CODE,
                    "Sort field is not allowed"
            );
        }

        Sort.Direction direction = Sort.Direction.fromOptionalString(parts[1].trim())
                .orElseThrow(() -> new BadRequestException(
                        ErrorMessages.REQUEST_PARAMETER_INVALID_CODE,
                        "Sort direction must be ASC or DESC"
                ));

        return Sort.by(direction, property);
    }

    private void validatePatch(UUID projectId, UpdateTaskRequest request, MemberRole actorRole, Task task) {
        TaskStatus nextStatus = request.status() == null ? task.getStatus() : request.status();
        Priority nextPriority = request.priority() == null ? task.getPriority() : request.priority();
        UUID nextAssignee = request.assigneeId() == null ? task.getUserId() : request.assigneeId();

        validateAssignee(projectId, nextAssignee);
        validateDoneToTodo(task.getStatus(), nextStatus);
        validateCriticalDone(nextPriority, nextStatus, actorRole);
        validateWipLimit(projectId, nextAssignee, nextStatus, task);
    }

    private void applyPatch(Task task, UpdateTaskRequest request, UUID actorId) {
        PatchChanges changes = PatchChanges.from(task, request);

        applyGeneralChanges(task, request, actorId, changes);
        applyStatusChange(task, request, actorId, changes);
        applyPriorityChange(task, request, actorId, changes);
        applyAssigneeChange(task, request, actorId, changes);
        applyDueDateChange(task, request, actorId, changes);
    }

    private void applyGeneralChanges(Task task, UpdateTaskRequest request, UUID actorId, PatchChanges changes) {
        if (!changes.generalChanged()) return;

        if (changes.titleChanged()) task.setTitle(request.title());
        if (changes.descriptionChanged()) task.setDescription(request.description());

        saveAudit(task, actorId, AuditAction.TASK_UPDATED, null, null);
    }

    private void applyStatusChange(Task task, UpdateTaskRequest request, UUID actorId, PatchChanges changes) {
        if (!changes.statusChanged()) return;

        TaskStatus previousStatus = task.getStatus();
        task.setStatus(request.status());

        saveAudit(task, actorId, AuditAction.STATUS_CHANGED, previousStatus, task.getStatus());
    }

    private void applyPriorityChange(Task task, UpdateTaskRequest request, UUID actorId, PatchChanges changes) {
        if (!changes.priorityChanged()) return;

        task.setPriority(request.priority());
        saveAudit(task, actorId, AuditAction.PRIORITY_CHANGED, null, null);
    }

    private void applyAssigneeChange(Task task, UpdateTaskRequest request, UUID actorId, PatchChanges changes) {
        if (!changes.assigneeChanged()) return;

        task.setUserId(request.assigneeId());
        saveAudit(task, actorId, AuditAction.ASSIGNEE_CHANGED, null, null);
        notifyAssignee(task, actorId);
    }

    private void applyDueDateChange(Task task, UpdateTaskRequest request, UUID actorId, PatchChanges changes) {
        if (!changes.dueDateChanged()) return;

        task.setDueDate(request.dueDate());
        saveAudit(task, actorId, AuditAction.DUE_DATE_CHANGED, null, null);
    }

    private Task requireTaskInProject(UUID projectId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .filter(foundTask -> foundTask.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.TASK_NOT_FOUND_CODE, ErrorMessages.TASK_NOT_FOUND_MESSAGE));

        if (!projectId.equals(task.getProjectId())) {
            throw new NotFoundException(ErrorMessages.TASK_NOT_FOUND_CODE, ErrorMessages.TASK_NOT_FOUND_MESSAGE);
        }

        return task;
    }

    private User resolveAssignee(UUID userId) {
        return userId == null ? null : userRepository.findById(userId).orElse(null);
    }

    private Map<UUID, User> findAssigneesByTasks(List<Task> tasks) {
        List<UUID> userIds = tasks.stream()
                .map(Task::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private void validateAssignee(UUID projectId, UUID assigneeId) {
        if (assigneeId != null && !projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)) {
            throw new BusinessException(ErrorMessages.TASK_ASSIGNEE_NOT_MEMBER_CODE, ErrorMessages.TASK_ASSIGNEE_NOT_MEMBER_MESSAGE);
        }
    }

    private void validateDoneToTodo(TaskStatus currentStatus, TaskStatus nextStatus) {
        if (currentStatus == TaskStatus.DONE && nextStatus == TaskStatus.TODO) {
            throw new BusinessException(ErrorMessages.TASK_DONE_TO_TODO_CODE, ErrorMessages.TASK_DONE_TO_TODO_MESSAGE);
        }
    }

    private void validateCriticalDone(Priority priority, TaskStatus status, MemberRole actorRole) {
        if (priority == Priority.CRITICAL && status == TaskStatus.DONE && actorRole != MemberRole.ADMIN) {
            throw new BusinessException(
                    ErrorMessages.TASK_CRITICAL_DONE_ADMIN_REQUIRED_CODE,
                    ErrorMessages.TASK_CRITICAL_DONE_ADMIN_REQUIRED_MESSAGE
            );
        }
    }

    private void validateWipLimit(UUID projectId, UUID assigneeId, TaskStatus status, Task currentTask) {
        if (assigneeId == null || status != TaskStatus.IN_PROGRESS) {
            return;
        }

        long count = taskRepository.countByProjectIdAndUserIdAndStatusAndDeletedAtIsNull(projectId, assigneeId, TaskStatus.IN_PROGRESS);

        if (countsCurrentTask(currentTask, assigneeId)) count--;
        if (count >= WIP_LIMIT)  throw new BusinessException(ErrorMessages.TASK_WIP_LIMIT_EXCEEDED_CODE, ErrorMessages.TASK_WIP_LIMIT_EXCEEDED_MESSAGE);
    }

    private boolean countsCurrentTask(Task currentTask, UUID assigneeId) {
        return currentTask != null
                && currentTask.getId() != null
                && currentTask.getStatus() == TaskStatus.IN_PROGRESS
                && assigneeId.equals(currentTask.getUserId());
    }

    private void saveAudit(Task task, UUID actorId, AuditAction action, TaskStatus fromStatus, TaskStatus toStatus) {
        taskLogRepository.save(TaskLog.builder()
                .projectId(task.getProjectId())
                .taskId(task.getId())
                .actorId(actorId)
                .action(action)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .build());
    }

    private void notifyAssignee(Task task, UUID actorId) {
        if (task.getUserId() == null) {
            return;
        }

        Notification notification = notificationRepository.save(Notification.builder()
                .type(NotificationType.TASK_ASSIGNED)
                .message("Task assigned: " + task.getTitle())
                .projectId(task.getProjectId())
                .taskId(task.getId())
                .createdBy(actorId)
                .build());

        userNotificationRepository.save(UserNotification.builder()
                .id(UserNotification.Id.builder()
                        .userId(task.getUserId())
                        .notificationId(notification.getId())
                        .build())
                .readAt(null)
                .build());
    }

    private record PatchChanges(
            boolean titleChanged,
            boolean descriptionChanged,
            boolean statusChanged,
            boolean priorityChanged,
            boolean assigneeChanged,
            boolean dueDateChanged
    ) {
        static PatchChanges from(Task task, UpdateTaskRequest request) {
            return new PatchChanges(
                    request.title() != null && !Objects.equals(task.getTitle(), request.title()),
                    request.description() != null && !Objects.equals(task.getDescription(), request.description()),
                    request.status() != null && task.getStatus() != request.status(),
                    request.priority() != null && task.getPriority() != request.priority(),
                    request.assigneeId() != null && !Objects.equals(task.getUserId(), request.assigneeId()),
                    request.dueDate() != null && !Objects.equals(task.getDueDate(), request.dueDate())
            );
        }

        boolean generalChanged() {
            return titleChanged || descriptionChanged;
        }
    }
}
