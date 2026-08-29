package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.ProjectMember;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import com.elotech.taskmanager.repository.project.ProjectSpecifications;
import com.elotech.taskmanager.repository.projectmember.ProjectMemberRepository;
import com.elotech.taskmanager.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findAllByMemberUserIdFiltersByNameAndDescriptionIgnoringCaseAndMembership() {
        User currentUser = userRepository.save(user("Maria", "maria@example.com"));
        User otherUser = userRepository.save(user("Joao", "joao@example.com"));
        Project matchingProject = project("Plataforma Interna", "Backlog da equipe");
        Project wrongDescription = project("Plataforma Comercial", "Roadmap comercial");
        Project inaccessible = project("Plataforma Interna", "Backlog da equipe");

        projectMemberRepository.save(member(matchingProject, currentUser));
        projectMemberRepository.save(member(wrongDescription, currentUser));
        projectMemberRepository.save(member(inaccessible, otherUser));

        Page<Project> response = projectRepository.findAll(
                ProjectSpecifications.byMemberAndFilters(currentUser.getId(), "plataforma", "BACKLOG"),
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent())
                .extracting(Project::getId)
                .containsExactly(matchingProject.getId());
    }

    private Project project(String name, String description) {
        User owner = userRepository.save(user(name + " owner", UUID.randomUUID() + "@example.com"));
        return projectRepository.save(Project.builder()
                .name(name)
                .description(description)
                .ownerId(owner.getId())
                .build());
    }

    private ProjectMember member(Project project, User user) {
        return ProjectMember.builder()
                .projectId(project.getId())
                .userId(user.getId())
                .role(MemberRole.MEMBER)
                .build();
    }

    private User user(String name, String email) {
        return User.builder()
                .name(name)
                .email(email)
                .password("hash")
                .build();
    }
}
