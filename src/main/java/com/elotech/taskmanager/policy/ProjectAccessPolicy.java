package com.elotech.taskmanager.policy;

import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import com.elotech.taskmanager.domain.error.ForbiddenException;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.repository.ProjectMemberRepository;
import com.elotech.taskmanager.repository.ProjectRepository;
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
                .orElseThrow(() -> new NotFoundException("error.project.not-found", "Project not found"));
    }

    public Project requireAdmin(UUID projectId, UUID userId) {
        Project project = requireMember(projectId, userId);
        MemberRole role = projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException("error.project.not-found", "Project not found"));

        if (role != MemberRole.ADMIN) {
            throw new ForbiddenException("error.project.admin-required", "Only project admins can perform this action");
        }

        return project;
    }

    public MemberRole requireRole(UUID projectId, UUID userId) {
        return projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException("error.project.not-found", "Project not found"));
    }
}
