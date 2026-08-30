package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardProjectResponse;
import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardResponse;
import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardWipAssigneeResponse;
import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardWipResponse;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import com.elotech.taskmanager.repository.task.TaskPriorityCountProjection;
import com.elotech.taskmanager.repository.task.TaskRepository;
import com.elotech.taskmanager.repository.task.TaskStatusCountProjection;
import com.elotech.taskmanager.repository.task.TaskWipAssigneeProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private static final int DUE_SOON_DAYS = 7;

    private final CurrentUserService currentUserService;
    private final ProjectAccessPolicy projectAccessPolicy;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public DashboardService(
            CurrentUserService currentUserService,
            ProjectAccessPolicy projectAccessPolicy,
            ProjectRepository projectRepository,
            TaskRepository taskRepository
    ) {
        this.currentUserService = currentUserService;
        this.projectAccessPolicy = projectAccessPolicy;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID projectId) {
        User currentUser = currentUserService.getCurrentUser();
        UUID userId = currentUser.getId();
        List<DashboardProjectResponse> projects = projectRepository.findDashboardProjectsByMemberUserId(userId);

        if (projectId != null) {
            projectAccessPolicy.requireMember(projectId, userId);
        }

        Map<TaskStatus, Long> byStatus = emptyStatusMap();
        statusCounts(userId, projectId).forEach(count -> byStatus.put(count.getStatus(), count.getTotal()));

        Map<Priority, Long> byPriority = emptyPriorityMap();
        priorityCounts(userId, projectId).forEach(count -> byPriority.put(count.getPriority(), count.getTotal()));

        LocalDateTime now = LocalDateTime.now();
        long overdue = overdueCount(userId, projectId, now);
        long dueSoon = dueSoonCount(userId, projectId, now, now.plusDays(DUE_SOON_DAYS));
        long tasksTotal = byStatus.values().stream().mapToLong(Long::longValue).sum();

        return new DashboardResponse(
                projects.size(),
                tasksTotal,
                byStatus,
                byPriority,
                overdue,
                dueSoon,
                projects,
                projectId,
                now
        );
    }

    @Transactional(readOnly = true)
    public DashboardWipResponse getWip(UUID projectId) {
        User currentUser = currentUserService.getCurrentUser();
        UUID userId = currentUser.getId();

        if (projectId != null) {
            projectAccessPolicy.requireMember(projectId, userId);
        }

        List<DashboardWipAssigneeResponse> items = wipCounts(userId, projectId).stream()
                .map(count -> new DashboardWipAssigneeResponse(
                        count.getUserId(),
                        count.getName(),
                        count.getEmail(),
                        count.getInProgress()
                ))
                .toList();

        return new DashboardWipResponse(items, projectId, LocalDateTime.now());
    }

    private List<TaskStatusCountProjection> statusCounts(UUID userId, UUID projectId) {
        if (projectId == null) {
            return taskRepository.countByStatusForMemberProjects(userId);
        }
        return taskRepository.countByStatusForMemberProject(userId, projectId);
    }

    private List<TaskPriorityCountProjection> priorityCounts(UUID userId, UUID projectId) {
        if (projectId == null) {
            return taskRepository.countByPriorityForMemberProjects(userId);
        }
        return taskRepository.countByPriorityForMemberProject(userId, projectId);
    }

    private long overdueCount(UUID userId, UUID projectId, LocalDateTime now) {
        if (projectId == null) return taskRepository.countOverdueForMemberProjects(userId, now, TaskStatus.DONE);
        return taskRepository.countOverdueForMemberProject(userId, projectId, now, TaskStatus.DONE);
    }

    private long dueSoonCount(UUID userId, UUID projectId, LocalDateTime now, LocalDateTime dueSoonLimit) {
        if (projectId == null) return taskRepository.countDueSoonForMemberProjects(userId, now, dueSoonLimit, TaskStatus.DONE);
        return taskRepository.countDueSoonForMemberProject(userId, projectId, now, dueSoonLimit, TaskStatus.DONE);
    }

    private List<TaskWipAssigneeProjection> wipCounts(UUID userId, UUID projectId) {
        if (projectId == null) return taskRepository.countWipByAssigneeForMemberProjects(userId, TaskStatus.IN_PROGRESS);
        return taskRepository.countWipByAssigneeForMemberProject(userId, projectId, TaskStatus.IN_PROGRESS);
    }

    private Map<TaskStatus, Long> emptyStatusMap() {
        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        Arrays.stream(TaskStatus.values()).forEach(status -> counts.put(status, 0L));
        return counts;
    }

    private Map<Priority, Long> emptyPriorityMap() {
        Map<Priority, Long> counts = new EnumMap<>(Priority.class);
        Arrays.stream(Priority.values()).forEach(priority -> counts.put(priority, 0L));
        return counts;
    }
}
