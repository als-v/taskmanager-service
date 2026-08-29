package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.audit.AuditLogResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.pagination.PageRequests;
import org.springframework.data.domain.PageRequest;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.TaskLogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private final TaskLogRepository taskLogRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAccessPolicy projectAccessPolicy;

    public AuditLogService(TaskLogRepository taskLogRepository, CurrentUserService currentUserService, ProjectAccessPolicy projectAccessPolicy) {
        this.taskLogRepository = taskLogRepository;
        this.currentUserService = currentUserService;
        this.projectAccessPolicy = projectAccessPolicy;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(UUID projectId, UUID taskId, UUID actorId, AuditAction action, Integer page, Integer size) {
        User currentUser = currentUserService.getCurrentUser();
        projectAccessPolicy.requireMember(projectId, currentUser.getId());
        PageRequest pageRequest = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return PageResponse.from(taskLogRepository.findByProjectIdWithFilters(projectId, taskId, actorId, action, pageRequest).map(AuditLogResponse::from));
    }
}
