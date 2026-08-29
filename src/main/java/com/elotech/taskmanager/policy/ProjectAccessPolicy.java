package com.elotech.taskmanager.policy;

import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.domain.error.ForbiddenException;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.repository.projectmember.ProjectMemberRepository;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProjectAccessPolicy {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAccessPolicy(ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public Project requireMember(UUID projectId, UUID userId) {
        return projectRepository.findByIdAndMemberUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.PROJECT_NOT_FOUND_CODE, ErrorMessages.PROJECT_NOT_FOUND_MESSAGE));
    }

    public Project requireAdmin(UUID projectId, UUID userId) {
        Project project = requireMember(projectId, userId);
        MemberRole role = projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.PROJECT_NOT_FOUND_CODE, ErrorMessages.PROJECT_NOT_FOUND_MESSAGE));

        if (role != MemberRole.ADMIN) {
            throw new ForbiddenException(ErrorMessages.PROJECT_ADMIN_REQUIRED_CODE, ErrorMessages.PROJECT_ADMIN_REQUIRED_MESSAGE);
        }

        return project;
    }

    public MemberRole requireRole(UUID projectId, UUID userId) {
        return projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.PROJECT_NOT_FOUND_CODE, ErrorMessages.PROJECT_NOT_FOUND_MESSAGE));
    }
}
