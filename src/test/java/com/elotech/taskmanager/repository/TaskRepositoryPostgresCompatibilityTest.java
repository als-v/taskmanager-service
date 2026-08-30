package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.domain.criteria.TaskListCriteria;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import com.elotech.taskmanager.repository.task.TaskRepository;
import com.elotech.taskmanager.repository.task.TaskSpecifications;
import com.elotech.taskmanager.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/taskmanager",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres"
})
@EnabledIfSystemProperty(named = "postgres.compatibility.enabled", matches = "true")
class TaskRepositoryPostgresCompatibilityTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void listWithoutOptionalFiltersWorksOnPostgres() {
        User owner = userRepository.save(User.builder()
                .name("Owner Postgres")
                .email("owner-postgres-compat@example.com")
                .password("hash")
                .build());
        Project project = projectRepository.save(Project.builder()
                .name("Projeto Postgres")
                .ownerId(owner.getId())
                .build());
        Task expected = taskRepository.save(Task.builder()
                .projectId(project.getId())
                .title("Listagem sem filtros")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .build());

        Page<Task> response = taskRepository.findAll(
                TaskSpecifications.byCriteria(project.getId(), new TaskListCriteria(null, null, null, null, null, null, null, null, null, null)),
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent())
                .extracting(Task::getId)
                .contains(expected.getId());
    }
}
