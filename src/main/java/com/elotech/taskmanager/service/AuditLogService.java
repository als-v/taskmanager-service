package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.audit.AuditLogResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.pagination.PageRequests;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.tasklog.TaskLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditLogService {

    private final TaskLogRepository taskLogRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAccessPolicy projectAccessPolicy;

    public AuditLogService(
            TaskLogRepository taskLogRepository,
            CurrentUserService currentUserService,
            ProjectAccessPolicy projectAccessPolicy
    ) {
        this.taskLogRepository = taskLogRepository;
        this.currentUserService = currentUserService;
        this.projectAccessPolicy = projectAccessPolicy;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(
            UUID projectId,
            UUID taskId,
            UUID actorId,
            AuditAction action,
            LocalDateTime createdAtFrom,
            LocalDateTime createdAtTo,
            Integer page,
            Integer size
    ) {
        User currentUser = currentUserService.getCurrentUser();
        projectAccessPolicy.requireMember(projectId, currentUser.getId());
        validateCreatedAtRange(createdAtFrom, createdAtTo);

        PageRequest pageRequest = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return PageResponse.from(taskLogRepository.findByProjectIdWithFilters(
                projectId,
                taskId,
                actorId,
                action,
                createdAtFrom,
                createdAtTo,
                pageRequest
        ).map(AuditLogResponse::from));
    }

    private void validateCreatedAtRange(LocalDateTime createdAtFrom, LocalDateTime createdAtTo) {
        if (createdAtFrom != null && createdAtTo != null && createdAtFrom.isAfter(createdAtTo)) {
            throw new BadRequestException(
                    ErrorMessages.REQUEST_PARAMETER_INVALID_CODE,
                    "created_at_from must be before or equal to created_at_to"
            );
        }
    }
}
