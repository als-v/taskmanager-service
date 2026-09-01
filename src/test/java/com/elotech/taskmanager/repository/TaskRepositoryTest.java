package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.criteria.TaskListCriteria;

import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import com.elotech.taskmanager.repository.task.TaskRepository;
import com.elotech.taskmanager.repository.task.TaskSpecifications;
import com.elotech.taskmanager.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;


    @Test
    void findAllByCriteriaAcceptsNullOptionalFilters() {
        User owner = userRepository.save(user("Owner", "owner-task-null-filters@example.com"));
        Project project = projectRepository.save(project(owner.getId()));
        Task expected = task(project.getId(), owner.getId(), "Listar tasks", null, TaskStatus.TODO, Priority.MEDIUM, null, null);
        taskRepository.save(expected);

        Page<Task> response = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(null, null, null, null, null, null, null, null, null, null)),
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent())
                .extracting(Task::getId)
                .containsExactly(expected.getId());
    }

    @Test
    void findAllByCriteriaFiltersByQInTitleIgnoringCase() {
        User owner = userRepository.save(user("Owner", "owner-task-filter@example.com"));
        Project project = projectRepository.save(project(owner.getId()));
        Task expected = task(project.getId(), owner.getId(), "Implementar Login", "Criar REFRESH token", TaskStatus.TODO, Priority.HIGH, LocalDateTime.of(2026, 2, 15, 18, 0), null);
        taskRepository.save(expected);
        taskRepository.save(task(project.getId(), owner.getId(), "Ajustar relatório", "Listar tarefas", TaskStatus.TODO, Priority.HIGH, LocalDateTime.of(2026, 2, 16, 18, 0), null));

        Page<Task> response = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(null, null, null, null, null, "login", null, null, null, null)),
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent())
                .extracting(Task::getId)
                .containsExactly(expected.getId());
    }

    @Test
    void findAllByCriteriaFiltersByQInDescriptionIgnoringCase() {
        User owner = userRepository.save(user("Owner", "owner-task-filter-description@example.com"));
        Project project = projectRepository.save(project(owner.getId()));
        Task expected = task(project.getId(), owner.getId(), "Implementar auth", "Criar REFRESH token", TaskStatus.TODO, Priority.HIGH, null, null);
        taskRepository.save(expected);
        taskRepository.save(task(project.getId(), owner.getId(), "Ajustar relatório", "Listar tarefas", TaskStatus.TODO, Priority.HIGH, null, null));

        Page<Task> response = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(null, null, null, null, null, "refresh", null, null, null, null)),
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent())
                .extracting(Task::getId)
                .containsExactly(expected.getId());
    }

    @Test
    void findAllByCriteriaEscapesQWildcardsAsLiteralText() {
        User owner = userRepository.save(user("Owner", "owner-task-filter-literal@example.com"));
        Project project = projectRepository.save(project(owner.getId()));
        Task percentTask = task(project.getId(), owner.getId(), "Taxa 100% pronta", null, TaskStatus.TODO, Priority.MEDIUM, null, null);
        Task underscoreTask = task(project.getId(), owner.getId(), "Codigo task", "token_api", TaskStatus.TODO, Priority.MEDIUM, null, null);
        taskRepository.save(percentTask);
        taskRepository.save(underscoreTask);
        taskRepository.save(task(project.getId(), owner.getId(), "Sem marcador", "texto comum", TaskStatus.TODO, Priority.MEDIUM, null, null));

        Page<Task> percentResponse = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(null, null, null, null, null, "%", null, null, null, null)),
                PageRequest.of(0, 20)
        );
        Page<Task> underscoreResponse = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(null, null, null, null, null, "_", null, null, null, null)),
                PageRequest.of(0, 20)
        );

        assertThat(percentResponse.getContent())
                .extracting(Task::getId)
                .containsExactly(percentTask.getId());
        assertThat(underscoreResponse.getContent())
                .extracting(Task::getId)
                .containsExactly(underscoreTask.getId());
    }

    @Test
    void findAllByCriteriaCombinesFiltersAndIgnoresSoftDeletedTasks() {
        User owner = userRepository.save(user("Owner", "owner-task-combined@example.com"));
        User assignee = userRepository.save(user("Assignee", "assignee-task-combined@example.com"));
        Project project = projectRepository.save(project(owner.getId()));
        LocalDateTime from = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 28, 23, 59);
        Task expected = task(project.getId(), assignee.getId(), "API de tasks", "Busca por descricao", TaskStatus.IN_PROGRESS, Priority.CRITICAL, LocalDateTime.of(2026, 2, 15, 18, 0), null);
        taskRepository.save(expected);
        taskRepository.save(task(project.getId(), assignee.getId(), "API de tasks removida", "Busca por descricao", TaskStatus.IN_PROGRESS, Priority.CRITICAL, LocalDateTime.of(2026, 2, 15, 18, 0), LocalDateTime.now()));
        taskRepository.save(task(project.getId(), assignee.getId(), "API de tasks", "Busca por descricao", TaskStatus.TODO, Priority.CRITICAL, LocalDateTime.of(2026, 2, 15, 18, 0), null));

        Page<Task> response = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(
                        TaskStatus.IN_PROGRESS, Priority.CRITICAL, assignee.getId(), from, to, "tasks", null, null, null, null
                )),
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent())
                .extracting(Task::getId)
                .containsExactly(expected.getId());
    }

    @Test
    void findAllByCriteriaFiltersUnassignedTasks() {
        User owner = userRepository.save(user("Owner", "owner-task-unassigned@example.com"));
        User assignee = userRepository.save(user("Assignee", "assignee-task-unassigned@example.com"));
        Project project = projectRepository.save(project(owner.getId()));
        Task unassigned = task(project.getId(), null, "Sem responsavel", null, TaskStatus.TODO, Priority.MEDIUM, null, null);
        Task assigned = task(project.getId(), assignee.getId(), "Com responsavel", null, TaskStatus.TODO, Priority.MEDIUM, null, null);
        taskRepository.save(unassigned);
        taskRepository.save(assigned);

        Page<Task> response = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(
                        null, null, null, null, null, null, null, null, null, true
                )),
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent())
                .extracting(Task::getId)
                .containsExactly(unassigned.getId());
    }

    @Test
    void findAllByCriteriaFiltersAssignedTasksWhenUnassignedFalse() {
        User owner = userRepository.save(user("Owner", "owner-task-unassigned-false@example.com"));
        User assignee = userRepository.save(user("Assignee", "assignee-task-unassigned-false@example.com"));
        Project project = projectRepository.save(project(owner.getId()));
        Task assigned = task(project.getId(), assignee.getId(), "Com responsavel", null, TaskStatus.TODO, Priority.MEDIUM, null, null);
        taskRepository.save(assigned);
        taskRepository.save(task(project.getId(), null, "Sem responsavel", null, TaskStatus.TODO, Priority.MEDIUM, null, null));

        Page<Task> response = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(
                        null, null, null, null, null, null, null, null, null, false
                )),
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent())
                .extracting(Task::getId)
                .containsExactly(assigned.getId());
    }

    @Test
    void findAllByCriteriaAssigneeIdTakesPrecedenceOverUnassignedFalse() {
        User owner = userRepository.save(user("Owner", "owner-task-unassigned-precedence@example.com"));
        User assignee = userRepository.save(user("Assignee", "assignee-task-unassigned-precedence@example.com"));
        Project project = projectRepository.save(project(owner.getId()));
        Task assigned = task(project.getId(), assignee.getId(), "Com responsavel", null, TaskStatus.TODO, Priority.MEDIUM, null, null);
        taskRepository.save(assigned);

        Page<Task> response = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(
                        null, null, assignee.getId(), null, null, null, null, null, null, false
                )),
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent())
                .extracting(Task::getId)
                .containsExactly(assigned.getId());
    }

    @Test
    void findAllByCriteriaOrdersByPriorityAscendingUsingSemanticHierarchy() {
        User owner = userRepository.save(user("Owner", "owner-task-priority-sort-asc@example.com"));
        Project project = projectRepository.save(project(owner.getId()));

        Task critical = task(project.getId(), owner.getId(), "Task critical", null, TaskStatus.TODO, Priority.CRITICAL, null, null);
        Task low = task(project.getId(), owner.getId(), "Task low", null, TaskStatus.TODO, Priority.LOW, null, null);
        Task high = task(project.getId(), owner.getId(), "Task high", null, TaskStatus.TODO, Priority.HIGH, null, null);
        Task medium = task(project.getId(), owner.getId(), "Task medium", null, TaskStatus.TODO, Priority.MEDIUM, null, null);

        taskRepository.save(critical);
        taskRepository.save(low);
        taskRepository.save(high);
        taskRepository.save(medium);

        Page<Task> response = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(null, null, null, null, null, null, null, null, null, null)),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "priorityRank"))
        );

        assertThat(response.getContent())
                .extracting(Task::getPriority)
                .containsExactly(Priority.LOW, Priority.MEDIUM, Priority.HIGH, Priority.CRITICAL);
    }

    @Test
    void findAllByCriteriaOrdersByPriorityDescendingUsingSemanticHierarchy() {
        User owner = userRepository.save(user("Owner", "owner-task-priority-sort-desc@example.com"));
        Project project = projectRepository.save(project(owner.getId()));

        Task medium = task(project.getId(), owner.getId(), "Task medium", null, TaskStatus.TODO, Priority.MEDIUM, null, null);
        Task critical = task(project.getId(), owner.getId(), "Task critical", null, TaskStatus.TODO, Priority.CRITICAL, null, null);
        Task low = task(project.getId(), owner.getId(), "Task low", null, TaskStatus.TODO, Priority.LOW, null, null);
        Task high = task(project.getId(), owner.getId(), "Task high", null, TaskStatus.TODO, Priority.HIGH, null, null);

        taskRepository.save(medium);
        taskRepository.save(critical);
        taskRepository.save(low);
        taskRepository.save(high);

        Page<Task> response = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(null, null, null, null, null, null, null, null, null, null)),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "priorityRank"))
        );

        assertThat(response.getContent())
                .extracting(Task::getPriority)
                .containsExactly(Priority.CRITICAL, Priority.HIGH, Priority.MEDIUM, Priority.LOW);
    }

    private User user(String name, String email) {
        return User.builder()
                .name(name)
                .email(email)
                .password("hash")
                .build();
    }

    private Project project(UUID ownerId) {
        return Project.builder()
                .name("Projeto")
                .description("Descricao")
                .ownerId(ownerId)
                .build();
    }

    private Task task(
            UUID projectId,
            UUID assigneeId,
            String title,
            String description,
            TaskStatus status,
            Priority priority,
            LocalDateTime dueDate,
            LocalDateTime deletedAt
    ) {
        return Task.builder()
                .projectId(projectId)
                .userId(assigneeId)
                .title(title)
                .description(description)
                .status(status)
                .priority(priority)
                .dueDate(dueDate)
                .deletedAt(deletedAt)
                .build();
    }
}
