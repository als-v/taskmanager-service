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
import com.elotech.taskmanager.repository.ProjectMemberRepository;
import com.elotech.taskmanager.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAccessPolicy projectAccessPolicy;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository, CurrentUserService currentUserService, ProjectAccessPolicy projectAccessPolicy) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.currentUserService = currentUserService;
        this.projectAccessPolicy = projectAccessPolicy;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(Integer page, Integer size) {
        User currentUser = currentUserService.getCurrentUser();
        PageRequest pageRequest = pageRequest(page, size);

        return PageResponse
                .from(
                        projectRepository
                                .findAllByMemberUserId(currentUser.getId(), pageRequest)
                                .map(project -> ProjectResponse.from(project, projectAccessPolicy.requireRole(project.getId(), currentUser.getId())))
                );
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId) {
        User currentUser = currentUserService.getCurrentUser();
        Project project = projectAccessPolicy.requireMember(projectId, currentUser.getId());
        MemberRole role = projectAccessPolicy.requireRole(projectId, currentUser.getId());

        return ProjectResponse.from(project, role);
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Project project = Project.builder().name(request.name()).description(request.description()).ownerId(currentUser.getId()).build();

        projectRepository.save(project);
        projectMemberRepository.save(ProjectMember.builder().projectId(project.getId()).userId(currentUser.getId()).role(MemberRole.ADMIN).build());

        return ProjectResponse.from(project, MemberRole.ADMIN);
    }

    @Transactional
    public ProjectResponse patch(UUID projectId, UpdateProjectRequest request) {
        if (request.hasNoChanges()) {
            throw new BadRequestException("error.project.no-fields", "At least one field must be provided");
        }

        User currentUser = currentUserService.getCurrentUser();
        log.debug("Updating project {} requested by user {}. Fields: name={}, description={}", projectId, currentUser.getId(), request.name() != null, request.description() != null);
        Project project = projectAccessPolicy.requireAdmin(projectId, currentUser.getId());

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("error.project.name-blank", "Project name cannot be blank");
            }
            project.setName(request.name());
        }

        if (request.description() != null) {
            project.setDescription(request.description());
        }

        return ProjectResponse.from(project, MemberRole.ADMIN);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        int pageValue = page == null ? DEFAULT_PAGE : page;
        int sizeValue = size == null ? DEFAULT_SIZE : size;

        if (pageValue < 0) {
            throw new BadRequestException("error.pagination.page-invalid", "Page must be greater than or equal to zero");
        }
        if (sizeValue < 1) {
            throw new BadRequestException("error.pagination.size-invalid", "Size must be greater than zero");
        }

        return PageRequest.of(pageValue, Math.min(sizeValue, MAX_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
