package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardProjectResponse;
import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.ProjectMember;
import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import com.elotech.taskmanager.repository.projectmember.ProjectMemberRepository;
import com.elotech.taskmanager.repository.task.TaskRepository;
import com.elotech.taskmanager.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DashboardRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void dashboardQueriesAggregateOnlyActiveTasksFromMemberProjects() {
        User currentUser = userRepository.save(user("Maria", "maria-dashboard-repository@example.com"));
        User otherUser = userRepository.save(user("Joao", "joao-dashboard-repository@example.com"));

        Project api = projectRepository.save(project("API", currentUser.getId(), null));
        Project frontend = projectRepository.save(project("Frontend", currentUser.getId(), null));
        Project inaccessible = projectRepository.save(project("Inacessivel", otherUser.getId(), null));
        Project deleted = projectRepository.save(project("Removido", currentUser.getId(), LocalDateTime.now()));

        projectMemberRepository.save(member(api, currentUser, MemberRole.ADMIN));
        projectMemberRepository.save(member(frontend, currentUser, MemberRole.MEMBER));
        projectMemberRepository.save(member(inaccessible, otherUser, MemberRole.ADMIN));
        projectMemberRepository.save(member(deleted, currentUser, MemberRole.ADMIN));

        taskRepository.save(task(api, TaskStatus.TODO, Priority.HIGH, null));
        taskRepository.save(task(api, TaskStatus.IN_PROGRESS, Priority.CRITICAL, null));
        taskRepository.save(task(api, TaskStatus.DONE, Priority.MEDIUM, LocalDateTime.now()));
        taskRepository.save(task(frontend, TaskStatus.TODO, Priority.LOW, null));
        taskRepository.save(task(inaccessible, TaskStatus.DONE, Priority.HIGH, null));
        taskRepository.save(task(deleted, TaskStatus.DONE, Priority.HIGH, null));

        assertThat(projectRepository.findDashboardProjectsByMemberUserId(currentUser.getId()))
                .extracting(DashboardProjectResponse::name)
                .containsExactly("API", "Frontend");

        assertThat(taskRepository.countByStatusForMemberProjects(currentUser.getId()))
                .extracting(count -> count.getStatus().name() + ":" + count.getTotal())
                .containsExactlyInAnyOrder("TODO:2", "IN_PROGRESS:1");

        assertThat(taskRepository.countByPriorityForMemberProjects(currentUser.getId()))
                .extracting(count -> count.getPriority().name() + ":" + count.getTotal())
                .containsExactlyInAnyOrder("LOW:1", "HIGH:1", "CRITICAL:1");
    }

    @Test
    void dashboardQueriesCanAggregateOnlySelectedProject() {
        User currentUser = userRepository.save(user("Ana", "ana-dashboard-repository@example.com"));
        Project api = projectRepository.save(project("API", currentUser.getId(), null));
        Project frontend = projectRepository.save(project("Frontend", currentUser.getId(), null));
        projectMemberRepository.save(member(api, currentUser, MemberRole.ADMIN));
        projectMemberRepository.save(member(frontend, currentUser, MemberRole.MEMBER));

        taskRepository.save(task(api, TaskStatus.TODO, Priority.HIGH, null));
        taskRepository.save(task(frontend, TaskStatus.DONE, Priority.LOW, null));

        assertThat(taskRepository.countByStatusForMemberProject(currentUser.getId(), api.getId()))
                .extracting(count -> count.getStatus().name() + ":" + count.getTotal())
                .containsExactly("TODO:1");
        assertThat(taskRepository.countByPriorityForMemberProject(currentUser.getId(), api.getId()))
                .extracting(count -> count.getPriority().name() + ":" + count.getTotal())
                .containsExactly("HIGH:1");
    }

    private User user(String name, String email) {
        return User.builder()
                .name(name)
                .email(email)
                .password("hash")
                .build();
    }

    private Project project(String name, java.util.UUID ownerId, LocalDateTime deletedAt) {
        return Project.builder()
                .name(name)
                .description("Projeto")
                .ownerId(ownerId)
                .deletedAt(deletedAt)
                .build();
    }

    private ProjectMember member(Project project, User user, MemberRole role) {
        return ProjectMember.builder()
                .projectId(project.getId())
                .userId(user.getId())
                .role(role)
                .build();
    }

    private Task task(Project project, TaskStatus status, Priority priority, LocalDateTime deletedAt) {
        return Task.builder()
                .projectId(project.getId())
                .title("Task")
                .status(status)
                .priority(priority)
                .deletedAt(deletedAt)
                .build();
    }
}
