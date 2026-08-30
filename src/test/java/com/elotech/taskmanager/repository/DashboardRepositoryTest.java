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
import java.util.UUID;

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

        taskRepository.save(task(api, TaskStatus.TODO, Priority.HIGH, currentUser.getId(), LocalDateTime.now().plusDays(1), null));
        taskRepository.save(task(api, TaskStatus.IN_PROGRESS, Priority.CRITICAL, currentUser.getId(), LocalDateTime.now().plusDays(2), null));
        taskRepository.save(task(api, TaskStatus.DONE, Priority.MEDIUM, currentUser.getId(), LocalDateTime.now(), null));
        taskRepository.save(task(frontend, TaskStatus.TODO, Priority.LOW, null, LocalDateTime.now().plusDays(3), null));
        taskRepository.save(task(inaccessible, TaskStatus.DONE, Priority.HIGH, otherUser.getId(), LocalDateTime.now(), null));
        taskRepository.save(task(deleted, TaskStatus.DONE, Priority.HIGH, currentUser.getId(), LocalDateTime.now(), null));

        assertThat(projectRepository.findDashboardProjectsByMemberUserId(currentUser.getId()))
                .extracting(DashboardProjectResponse::name)
                .containsExactly("API", "Frontend");

        assertThat(taskRepository.countByStatusForMemberProjects(currentUser.getId()))
                .map(count -> count.getStatus().name() + ":" + count.getTotal())
                .containsExactlyInAnyOrder("TODO:2", "IN_PROGRESS:1", "DONE:1");

        assertThat(taskRepository.countByPriorityForMemberProjects(currentUser.getId()))
                .map(count -> count.getPriority().name() + ":" + count.getTotal())
                .containsExactlyInAnyOrder("LOW:1", "MEDIUM:1", "HIGH:1", "CRITICAL:1");
    }

    @Test
    void dashboardProjectQueriesRestrictTaskMetricsToSelectedProject() {
        User currentUser = userRepository.save(user("Ana", "ana-dashboard-repository@example.com"));

        Project api = projectRepository.save(project("API", currentUser.getId(), null));
        Project frontend = projectRepository.save(project("Frontend", currentUser.getId(), null));

        projectMemberRepository.save(member(api, currentUser, MemberRole.ADMIN));
        projectMemberRepository.save(member(frontend, currentUser, MemberRole.MEMBER));

        taskRepository.save(task(api, TaskStatus.TODO, Priority.HIGH, currentUser.getId(), LocalDateTime.now().plusDays(1), null));
        taskRepository.save(task(frontend, TaskStatus.DONE, Priority.LOW, currentUser.getId(), LocalDateTime.now().plusDays(1), null));

        assertThat(taskRepository.countByStatusForMemberProject(currentUser.getId(), api.getId()))
                .map(count -> count.getStatus().name() + ":" + count.getTotal())
                .containsExactly("TODO:1");

        assertThat(taskRepository.countByPriorityForMemberProject(currentUser.getId(), api.getId()))
                .map(count -> count.getPriority().name() + ":" + count.getTotal())
                .containsExactly("HIGH:1");
    }

    @Test
    void dashboardDeadlineQueriesExcludeDoneDeletedInactiveAndInaccessibleTasks() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 29, 10, 0);
        User currentUser = userRepository.save(user("Paulo", "paulo-dashboard-repository@example.com"));
        User otherUser = userRepository.save(user("Luiza", "luiza-dashboard-repository@example.com"));

        Project api = projectRepository.save(project("API", currentUser.getId(), null));
        Project frontend = projectRepository.save(project("Frontend", currentUser.getId(), null));
        Project inaccessible = projectRepository.save(project("Inacessivel", otherUser.getId(), null));
        Project deleted = projectRepository.save(project("Removido", currentUser.getId(), now));

        projectMemberRepository.save(member(api, currentUser, MemberRole.ADMIN));
        projectMemberRepository.save(member(frontend, currentUser, MemberRole.MEMBER));
        projectMemberRepository.save(member(inaccessible, otherUser, MemberRole.ADMIN));
        projectMemberRepository.save(member(deleted, currentUser, MemberRole.ADMIN));

        taskRepository.save(task(api, TaskStatus.TODO, Priority.HIGH, currentUser.getId(), now.minusHours(1), null));
        taskRepository.save(task(frontend, TaskStatus.IN_PROGRESS, Priority.MEDIUM, currentUser.getId(), now.plusDays(7), null));
        taskRepository.save(task(api, TaskStatus.DONE, Priority.HIGH, currentUser.getId(), now.minusDays(1), null));
        taskRepository.save(task(api, TaskStatus.DONE, Priority.HIGH, currentUser.getId(), now.plusDays(1), null));
        taskRepository.save(task(api, TaskStatus.TODO, Priority.LOW, currentUser.getId(), now.minusDays(1), now));
        taskRepository.save(task(api, TaskStatus.TODO, Priority.LOW, currentUser.getId(), now.plusDays(1), now));
        taskRepository.save(task(inaccessible, TaskStatus.TODO, Priority.HIGH, otherUser.getId(), now.minusDays(1), null));
        taskRepository.save(task(inaccessible, TaskStatus.TODO, Priority.HIGH, otherUser.getId(), now.plusDays(1), null));
        taskRepository.save(task(deleted, TaskStatus.TODO, Priority.HIGH, currentUser.getId(), now.minusDays(1), null));
        taskRepository.save(task(deleted, TaskStatus.TODO, Priority.HIGH, currentUser.getId(), now.plusDays(1), null));

        assertThat(taskRepository.countOverdueForMemberProjects(currentUser.getId(), now, TaskStatus.DONE)).isEqualTo(1);
        assertThat(taskRepository.countDueSoonForMemberProjects(currentUser.getId(), now, now.plusDays(7), TaskStatus.DONE)).isEqualTo(1);
        assertThat(taskRepository.countOverdueForMemberProject(currentUser.getId(), api.getId(), now, TaskStatus.DONE)).isEqualTo(1);
        assertThat(taskRepository.countDueSoonForMemberProject(currentUser.getId(), api.getId(), now, now.plusDays(7), TaskStatus.DONE)).isZero();
    }

    @Test
    void dashboardWipQueriesGroupOnlyInProgressAssignedTasksFromActiveAccessibleProjects() {
        User currentUser = userRepository.save(user("Carla", "carla-dashboard-repository@example.com"));
        User maria = userRepository.save(user("Maria", "maria-wip-dashboard-repository@example.com"));
        User ana = userRepository.save(user("Ana", "ana-wip-dashboard-repository@example.com"));
        User otherUser = userRepository.save(user("Bruno", "bruno-wip-dashboard-repository@example.com"));

        Project api = projectRepository.save(project("API", currentUser.getId(), null));
        Project frontend = projectRepository.save(project("Frontend", currentUser.getId(), null));
        Project inaccessible = projectRepository.save(project("Inacessivel", otherUser.getId(), null));
        Project deleted = projectRepository.save(project("Removido", currentUser.getId(), LocalDateTime.now()));

        projectMemberRepository.save(member(api, currentUser, MemberRole.ADMIN));
        projectMemberRepository.save(member(api, maria, MemberRole.MEMBER));
        projectMemberRepository.save(member(api, ana, MemberRole.MEMBER));
        projectMemberRepository.save(member(frontend, currentUser, MemberRole.MEMBER));
        projectMemberRepository.save(member(frontend, maria, MemberRole.MEMBER));
        projectMemberRepository.save(member(inaccessible, otherUser, MemberRole.ADMIN));
        projectMemberRepository.save(member(deleted, currentUser, MemberRole.ADMIN));

        taskRepository.save(task(api, TaskStatus.IN_PROGRESS, Priority.HIGH, maria.getId(), LocalDateTime.now().plusDays(1), null));
        taskRepository.save(task(api, TaskStatus.IN_PROGRESS, Priority.HIGH, maria.getId(), LocalDateTime.now().plusDays(2), null));
        taskRepository.save(task(frontend, TaskStatus.IN_PROGRESS, Priority.MEDIUM, ana.getId(), LocalDateTime.now().plusDays(3), null));
        taskRepository.save(task(api, TaskStatus.TODO, Priority.HIGH, maria.getId(), LocalDateTime.now().plusDays(1), null));
        taskRepository.save(task(api, TaskStatus.IN_PROGRESS, Priority.HIGH, null, LocalDateTime.now().plusDays(1), null));
        taskRepository.save(task(api, TaskStatus.IN_PROGRESS, Priority.HIGH, maria.getId(), LocalDateTime.now().plusDays(1), LocalDateTime.now()));
        taskRepository.save(task(inaccessible, TaskStatus.IN_PROGRESS, Priority.HIGH, otherUser.getId(), LocalDateTime.now().plusDays(1), null));
        taskRepository.save(task(deleted, TaskStatus.IN_PROGRESS, Priority.HIGH, maria.getId(), LocalDateTime.now().plusDays(1), null));

        assertThat(taskRepository.countWipByAssigneeForMemberProjects(currentUser.getId(), TaskStatus.IN_PROGRESS))
                .map(count -> count.getName() + ":" + count.getInProgress())
                .containsExactly("Ana:1", "Maria:2");

        assertThat(taskRepository.countWipByAssigneeForMemberProject(currentUser.getId(), api.getId(), TaskStatus.IN_PROGRESS))
                .map(count -> count.getName() + ":" + count.getInProgress())
                .containsExactly("Maria:2");
    }

    private User user(String name, String email) {
        return User.builder()
                .name(name)
                .email(email)
                .password("hash")
                .build();
    }

    private Project project(String name, UUID ownerId, LocalDateTime deletedAt) {
        return Project.builder()
                .name(name)
                .description(name + " description")
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

    private Task task(
            Project project,
            TaskStatus status,
            Priority priority,
            UUID assigneeId,
            LocalDateTime dueDate,
            LocalDateTime deletedAt
    ) {
        return Task.builder()
                .projectId(project.getId())
                .title("Task " + status.name())
                .description("Description")
                .status(status)
                .priority(priority)
                .userId(assigneeId)
                .dueDate(dueDate)
                .deletedAt(deletedAt)
                .build();
    }
}
