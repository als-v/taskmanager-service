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
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.pagination.PageRequests;
import org.springframework.data.domain.PageRequest;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.projectmember.ProjectMemberRepository;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import com.elotech.taskmanager.repository.project.ProjectSpecifications;
import com.elotech.taskmanager.repository.task.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final Sort DEFAULT_LIST_SORT = Sort.by(Sort.Direction.DESC, "updatedAt");
    private static final Map<String, String> LIST_SORT_FIELDS = Map.of(
            "name", "name",
            "created_at", "createdAt",
            "updated_at", "updatedAt"
    );

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAccessPolicy projectAccessPolicy;
    private final TaskRepository taskRepository;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository, CurrentUserService currentUserService, ProjectAccessPolicy projectAccessPolicy, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.currentUserService = currentUserService;
        this.projectAccessPolicy = projectAccessPolicy;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(String name, String description, String sort, Integer page, Integer size) {
        User currentUser = currentUserService.getCurrentUser();
        PageRequest pageRequest = PageRequests.of(page, size, sort, LIST_SORT_FIELDS, DEFAULT_LIST_SORT);

        return PageResponse
                .from(
                        projectRepository
                                .findAll(
                                        ProjectSpecifications.byMemberAndFilters(
                                                currentUser.getId(),
                                                blankToNull(name),
                                                blankToNull(description)
                                        ),
                                        pageRequest
                                )
                                .map(project -> ProjectResponse.from(project, projectAccessPolicy.requireRole(project.getId(), currentUser.getId())))
                );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
            throw new BadRequestException(ErrorMessages.PROJECT_NO_FIELDS_CODE, ErrorMessages.PROJECT_NO_FIELDS_MESSAGE);
        }

        User currentUser = currentUserService.getCurrentUser();
        log.debug("Updating project {} requested by user {}. Fields: name={}, description={}", projectId, currentUser.getId(), request.name() != null, request.description() != null);
        Project project = projectAccessPolicy.requireAdmin(projectId, currentUser.getId());

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException(ErrorMessages.PROJECT_NAME_BLANK_CODE, ErrorMessages.PROJECT_NAME_BLANK_MESSAGE);
            }
            project.setName(request.name());
        }

        if (request.description() != null) {
            project.setDescription(request.description());
        }

        return ProjectResponse.from(project, MemberRole.ADMIN);
    }


    @Transactional
    public void delete(UUID projectId) {
        User currentUser = currentUserService.getCurrentUser();
        Project project = projectAccessPolicy.requireAdmin(projectId, currentUser.getId());
        LocalDateTime deletedAt = LocalDateTime.now();
        project.setDeletedAt(deletedAt);

        taskRepository.updateDeletedAtByProjectIdAndDeletedAtIsNull(projectId, deletedAt);
    }
}
