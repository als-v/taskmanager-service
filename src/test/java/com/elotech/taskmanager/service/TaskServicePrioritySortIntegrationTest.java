package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.criteria.TaskListCriteria;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.task.TaskResponse;
import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.logging.ApplicationEventLogger;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.project.ProjectRepository;
import com.elotech.taskmanager.repository.task.TaskRepository;
import com.elotech.taskmanager.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@ActiveProfiles("test")
@Import(TaskService.class)
class TaskServicePrioritySortIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private ProjectAccessPolicy projectAccessPolicy;

    @MockitoBean
    private CacheInvalidationService cacheInvalidationService;

    @MockitoBean
    private ApplicationEventLogger eventLogger;

    @Test
    void listOrdersTasksByPriorityUsingSemanticHierarchyBothDirections() {
        User owner = userRepository.save(User.builder()
                .name("Owner")
                .email("owner-priority-sort-service@example.com")
                .password("hash")
                .build());
        Project project = projectRepository.save(Project.builder()
                .name("Projeto")
                .description("Descricao")
                .ownerId(owner.getId())
                .build());

        given(currentUserService.getCurrentUser()).willReturn(owner);

        taskRepository.save(task(project.getId(), "Task high", Priority.HIGH));
        taskRepository.save(task(project.getId(), "Task low", Priority.LOW));
        taskRepository.save(task(project.getId(), "Task critical", Priority.CRITICAL));
        taskRepository.save(task(project.getId(), "Task medium", Priority.MEDIUM));

        PageResponse<TaskResponse> ascending = taskService.list(project.getId(), new TaskListCriteria(
                null, null, null, null, null, null, "priority,asc", 0, 20, null));
        assertThat(ascending.content())
                .extracting(TaskResponse::priority)
                .containsExactly(Priority.LOW, Priority.MEDIUM, Priority.HIGH, Priority.CRITICAL);

        PageResponse<TaskResponse> descending = taskService.list(project.getId(), new TaskListCriteria(
                null, null, null, null, null, null, "priority,desc", 0, 20, null));
        assertThat(descending.content())
                .extracting(TaskResponse::priority)
                .containsExactly(Priority.CRITICAL, Priority.HIGH, Priority.MEDIUM, Priority.LOW);
    }

    private Task task(UUID projectId, String title, Priority priority) {
        return Task.builder()
                .projectId(projectId)
                .title(title)
                .status(TaskStatus.TODO)
                .priority(priority)
                .build();
    }
}
