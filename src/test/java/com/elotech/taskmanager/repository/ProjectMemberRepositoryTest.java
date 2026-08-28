package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.dto.response.member.ProjectMemberResponse;
import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.ProjectMember;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;


import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProjectMemberRepositoryTest {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findMembersByProjectIdOrdersByJoinedUserWithoutPageableSort() {
        User owner = userRepository.save(user("Owner", "owner@example.com"));
        User bruno = userRepository.save(user("Bruno", "bruno@example.com"));
        User ana = userRepository.save(user("Ana", "ana@example.com"));
        Project project = projectRepository.save(Project.builder()
                .name("Projeto")
                .description("Descricao")
                .ownerId(owner.getId())
                .build());

        projectMemberRepository.save(ProjectMember.builder()
                .projectId(project.getId())
                .userId(bruno.getId())
                .role(MemberRole.MEMBER)
                .build());
        projectMemberRepository.save(ProjectMember.builder()
                .projectId(project.getId())
                .userId(ana.getId())
                .role(MemberRole.MEMBER)
                .build());

        Page<ProjectMemberResponse> response = projectMemberRepository.findMembersByProjectId(
                project.getId(),
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent())
                .extracting(ProjectMemberResponse::name)
                .containsExactly("Ana", "Bruno");
    }

    private User user(String name, String email) {
        return User.builder()
                .name(name)
                .email(email)
                .password("hash")
                .build();
    }
}
