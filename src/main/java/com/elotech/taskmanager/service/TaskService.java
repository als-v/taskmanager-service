package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.request.task.CreateTaskRequest;
import com.elotech.taskmanager.domain.dto.request.task.UpdateTaskRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.task.TaskResponse;
import com.elotech.taskmanager.domain.entity.*;
import com.elotech.taskmanager.domain.enumeration.*;
import com.elotech.taskmanager.domain.error.*;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class TaskService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int WIP_LIMIT = 5;

    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAccessPolicy projectAccessPolicy;

    public TaskService(TaskRepository taskRepository, TaskLogRepository taskLogRepository, NotificationRepository notificationRepository, UserNotificationRepository userNotificationRepository, ProjectMemberRepository projectMemberRepository, CurrentUserService currentUserService, ProjectAccessPolicy projectAccessPolicy) {
        this.taskRepository = taskRepository;
        this.taskLogRepository = taskLogRepository;
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.currentUserService = currentUserService;
        this.projectAccessPolicy = projectAccessPolicy;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(UUID projectId, Integer page, Integer size) {
        User currentUser = currentUserService.getCurrentUser();
        projectAccessPolicy.requireMember(projectId, currentUser.getId());

        return PageResponse.from(taskRepository.findAllByProjectId(projectId, pageRequest(page, size)).map(TaskResponse::from));
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID projectId, UUID taskId) {
        User currentUser = currentUserService.getCurrentUser();
        projectAccessPolicy.requireMember(projectId, currentUser.getId());

        return TaskResponse.from(requireTaskInProject(projectId, taskId));
    }

    @Transactional
    public TaskResponse create(UUID projectId, CreateTaskRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        MemberRole actorRole = projectAccessPolicy.requireRole(projectId, currentUser.getId());

        validateAssignee(projectId, request.assigneeId());
        validateCriticalDone(request.priority(), request.status(), actorRole);
        validateWipLimit(projectId, request.assigneeId(), request.status(), null);

        Task task = Task
                .builder()
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

        return TaskResponse.from(saved);
    }

    @Transactional
    public TaskResponse patch(UUID projectId, UUID taskId, UpdateTaskRequest request) {
        if (request.hasNoChanges()) {
            throw new BadRequestException(ErrorMessages.TASK_NO_FIELDS_CODE, ErrorMessages.TASK_NO_FIELDS_MESSAGE);
        }

        User currentUser = currentUserService.getCurrentUser();
        MemberRole actorRole = projectAccessPolicy.requireRole(projectId, currentUser.getId());
        Task task = requireTaskInProject(projectId, taskId);

        if (request.title() != null && request.title().isBlank()) {
            throw new BadRequestException(ErrorMessages.TASK_TITLE_BLANK_CODE, ErrorMessages.TASK_TITLE_BLANK_MESSAGE);
        }

        TaskStatus nextStatus = request.status() == null ? task.getStatus() : request.status();
        Priority nextPriority = request.priority() == null ? task.getPriority() : request.priority();
        UUID nextAssignee = request.assigneeId() == null ? task.getUserId() : request.assigneeId();

        validateAssignee(projectId, nextAssignee);
        validateDoneToTodo(task.getStatus(), nextStatus);
        validateCriticalDone(nextPriority, nextStatus, actorRole);
        validateWipLimit(projectId, nextAssignee, nextStatus, task);

        TaskStatus previousStatus = task.getStatus();

        boolean titleChanged = request.title() != null && !Objects.equals(task.getTitle(), request.title());
        boolean descriptionChanged = request.description() != null && !Objects.equals(task.getDescription(), request.description());

        if (titleChanged || descriptionChanged) {
            if (titleChanged) task.setTitle(request.title());
            if (descriptionChanged) task.setDescription(request.description());
            saveAudit(task, currentUser.getId(), AuditAction.TASK_UPDATED, null, null);
        }

        if (request.status() != null && task.getStatus() != request.status()) {
            task.setStatus(request.status());
            saveAudit(task, currentUser.getId(), AuditAction.STATUS_CHANGED, previousStatus, task.getStatus());
        }

        if (request.priority() != null && task.getPriority() != request.priority()) {
            task.setPriority(request.priority());
            saveAudit(task, currentUser.getId(), AuditAction.PRIORITY_CHANGED, null, null);
        }

        if (request.assigneeId() != null && !Objects.equals(task.getUserId(), request.assigneeId())) {
            task.setUserId(request.assigneeId());
            saveAudit(task, currentUser.getId(), AuditAction.ASSIGNEE_CHANGED, null, null);
            notifyAssignee(task, currentUser.getId());
        }

        if (request.dueDate() != null && !Objects.equals(task.getDueDate(), request.dueDate())) {
            task.setDueDate(request.dueDate());
            saveAudit(task, currentUser.getId(), AuditAction.DUE_DATE_CHANGED, null, null);
        }

        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(UUID projectId, UUID taskId) {
        User currentUser = currentUserService.getCurrentUser();
        projectAccessPolicy.requireMember(projectId, currentUser.getId());
        MemberRole role = projectAccessPolicy.requireRole(projectId, currentUser.getId());

        if (role != MemberRole.ADMIN) {
            throw new ForbiddenException(ErrorMessages.TASK_NOT_FOUND_CODE, ErrorMessages.TASK_NOT_FOUND_MESSAGE);
        }

        Task task = requireTaskInProject(projectId, taskId);
        saveAudit(task, currentUser.getId(), AuditAction.TASK_DELETED, null, null);
        taskRepository.delete(task);
    }

    private Task requireTaskInProject(UUID projectId, UUID taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException(ErrorMessages.TASK_NOT_FOUND_CODE, ErrorMessages.TASK_NOT_FOUND_MESSAGE));

        if (!projectId.equals(task.getProjectId())) {
            throw new NotFoundException(ErrorMessages.TASK_NOT_FOUND_CODE, ErrorMessages.TASK_NOT_FOUND_MESSAGE);
        }

        return task;
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
            throw new BusinessException(ErrorMessages.TASK_CRITICAL_DONE_ADMIN_REQUIRED_CODE, ErrorMessages.TASK_CRITICAL_DONE_ADMIN_REQUIRED_MESSAGE);
        }
    }

    private void validateWipLimit(UUID projectId, UUID assigneeId, TaskStatus status, Task currentTask) {
        if (assigneeId == null || status != TaskStatus.IN_PROGRESS) {
            return;
        }

        long count = taskRepository.countByProjectIdAndUserIdAndStatus(projectId, assigneeId, TaskStatus.IN_PROGRESS);

        if (currentTask != null && currentTask.getId() != null && currentTask.getStatus() == TaskStatus.IN_PROGRESS && assigneeId.equals(currentTask.getUserId())) {
            count--;
        }

        if (count >= WIP_LIMIT) {
            throw new BusinessException(ErrorMessages.TASK_WIP_LIMIT_EXCEEDED_CODE, ErrorMessages.TASK_WIP_LIMIT_EXCEEDED_MESSAGE);
        }
    }

    private void saveAudit(Task task, UUID actorId, AuditAction action, TaskStatus fromStatus, TaskStatus toStatus) {
        taskLogRepository.save(TaskLog.builder().projectId(task.getProjectId()).taskId(task.getId()).actorId(actorId).action(action).fromStatus(fromStatus).toStatus(toStatus).build());
    }

    private void notifyAssignee(Task task, UUID actorId) {
        if (task.getUserId() == null) {
            return;
        }

        Notification notification = notificationRepository.save(Notification.builder().type(NotificationType.TASK_ASSIGNED).message("Task assigned: " + task.getTitle()).projectId(task.getProjectId()).taskId(task.getId()).createdBy(actorId).build());
        userNotificationRepository.save(UserNotification.builder().id(UserNotification.Id.builder().userId(task.getUserId()).notificationId(notification.getId()).build()).readAt(null).build());
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        int pageValue = page == null ? DEFAULT_PAGE : page;
        int sizeValue = size == null ? DEFAULT_SIZE : size;

        if (pageValue < 0) {
            throw new BadRequestException(ErrorMessages.PAGINATION_PAGE_INVALID_CODE, ErrorMessages.PAGINATION_PAGE_INVALID_MESSAGE);
        }

        if (sizeValue < 1) {
            throw new BadRequestException(ErrorMessages.PAGINATION_SIZE_INVALID_CODE, ErrorMessages.PAGINATION_SIZE_INVALID_MESSAGE);
        }

        return PageRequest.of(pageValue, Math.min(sizeValue, MAX_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
