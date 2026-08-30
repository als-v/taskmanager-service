package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.entity.TaskLog;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import com.elotech.taskmanager.repository.task.TaskRepository;
import com.elotech.taskmanager.repository.tasklog.TaskLogRepository;
import com.elotech.taskmanager.repository.tasklog.TaskLogWithReferences;
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
class TaskLogRepositoryTest {

    @Autowired
    private TaskLogRepository taskLogRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void findByProjectIdWithFiltersReturnsTaskAndActorNamesInsideCreatedAtRange() {
        User actor = userRepository.save(user("Maria Silva", "maria-log@example.com"));
        User otherActor = userRepository.save(user("Joao Silva", "joao-log@example.com"));
        Project project = projectRepository.save(project(actor.getId(), "Projeto Logs"));
        Project otherProject = projectRepository.save(project(otherActor.getId(), "Outro Projeto"));
        Task task = taskRepository.save(task(project.getId(), actor.getId(), "Criar endpoint de status"));
        Task otherTask = taskRepository.save(task(project.getId(), actor.getId(), "Outro card"));
        Task inaccessibleTask = taskRepository.save(task(otherProject.getId(), otherActor.getId(), "Card externo"));
        TaskLog expected = taskLogRepository.saveAndFlush(log(project.getId(), task.getId(), actor.getId(), AuditAction.STATUS_CHANGED));
        taskLogRepository.saveAndFlush(log(project.getId(), otherTask.getId(), actor.getId(), AuditAction.TASK_UPDATED));
        taskLogRepository.saveAndFlush(log(otherProject.getId(), inaccessibleTask.getId(), otherActor.getId(), AuditAction.STATUS_CHANGED));
        LocalDateTime createdAt = expected.getCreatedAt();

        Page<TaskLogWithReferences> response = taskLogRepository.findByProjectIdWithFilters(
                project.getId(),
                task.getId(),
                actor.getId(),
                AuditAction.STATUS_CHANGED,
                createdAt,
                createdAt,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(response.getContent()).hasSize(1);
        TaskLogWithReferences projection = response.getContent().get(0);
        assertThat(projection.log().getId()).isEqualTo(expected.getId());
        assertThat(projection.task().getTitle()).isEqualTo("Criar endpoint de status");
        assertThat(projection.actor().getName()).isEqualTo("Maria Silva");
    }

    @Test
    void findByProjectIdWithFiltersExcludesLogsOutsideCreatedAtRange() {
        User actor = userRepository.save(user("Ana Silva", "ana-log@example.com"));
        Project project = projectRepository.save(project(actor.getId(), "Projeto Intervalo"));
        Task task = taskRepository.save(task(project.getId(), actor.getId(), "Filtrar logs"));
        TaskLog log = taskLogRepository.saveAndFlush(log(project.getId(), task.getId(), actor.getId(), AuditAction.TASK_UPDATED));
        LocalDateTime afterLog = log.getCreatedAt().plusSeconds(1);
        LocalDateTime max = LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999);

        Page<TaskLogWithReferences> response = taskLogRepository.findByProjectIdWithFilters(
                project.getId(),
                null,
                null,
                null,
                afterLog,
                max,
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent()).isEmpty();
    }

    private User user(String name, String email) {
        return User.builder()
                .name(name)
                .email(email)
                .password("hash")
                .build();
    }

    private Project project(UUID ownerId, String name) {
        return Project.builder()
                .name(name)
                .description("Descricao")
                .ownerId(ownerId)
                .build();
    }

    private Task task(UUID projectId, UUID assigneeId, String title) {
        return Task.builder()
                .projectId(projectId)
                .userId(assigneeId)
                .title(title)
                .description("Descricao")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .build();
    }

    private TaskLog log(UUID projectId, UUID taskId, UUID actorId, AuditAction action) {
        return TaskLog.builder()
                .projectId(projectId)
                .taskId(taskId)
                .actorId(actorId)
                .action(action)
                .fromStatus(TaskStatus.TODO)
                .toStatus(TaskStatus.IN_PROGRESS)
                .build();
    }
}
