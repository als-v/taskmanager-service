package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.audit.AuditLogResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.TaskLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

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
        PageRequest pageRequest = pageRequest(page, size);

        return PageResponse.from(taskLogRepository.findByProjectIdWithFilters(projectId, taskId, actorId, action, pageRequest).map(AuditLogResponse::from));
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
