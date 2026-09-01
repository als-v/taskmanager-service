package com.elotech.taskmanager.config;

import com.elotech.taskmanager.domain.dto.request.auth.SignUpRequest;
import com.elotech.taskmanager.domain.dto.request.member.AddProjectMembersRequest;
import com.elotech.taskmanager.domain.dto.request.project.CreateProjectRequest;
import com.elotech.taskmanager.domain.dto.request.task.CreateTaskRequest;
import com.elotech.taskmanager.domain.dto.response.auth.UserResponse;
import com.elotech.taskmanager.domain.dto.response.project.ProjectResponse;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.service.AuthService;
import com.elotech.taskmanager.service.ProjectMemberService;
import com.elotech.taskmanager.service.ProjectService;
import com.elotech.taskmanager.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("e2e-seed")
public class E2eSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(E2eSeedRunner.class);

    public static final String ADMIN_EMAIL = "usuario@usuario.com";
    public static final String ASSIGNEE_EMAIL = "usuario2@usuario2.com";
    public static final String SEED_PASSWORD = "123mudarA@";
    public static final String PROJECT_NAME = "Projeto E2E";
    public static final String TASK_TITLE = "Tarefa E2E";

    public static final String BOARD_ASSIGNEE_EMAIL = "usuario3@usuario3.com";
    public static final String BOARD_PROJECT_NAME = "Projeto Board E2E";
    public static final String BOARD_DRAG_TASK_TITLE = "Tarefa Arraste E2E";
    public static final String BOARD_WIP_TASK_TITLE_PREFIX = "Tarefa WIP E2E ";
    public static final String BOARD_WIP_TARGET_TASK_TITLE = "Tarefa Alvo WIP E2E";
    private static final int WIP_LIMIT = 5;

    private final AuthService authService;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final TaskService taskService;

    public E2eSeedRunner(
            AuthService authService,
            ProjectService projectService,
            ProjectMemberService projectMemberService,
            TaskService taskService
    ) {
        this.authService = authService;
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
        this.taskService = taskService;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.signUp(new SignUpRequest("Admin E2E", ADMIN_EMAIL, SEED_PASSWORD));
        UserResponse assignee = authService.signUp(new SignUpRequest("Assignee E2E", ASSIGNEE_EMAIL, SEED_PASSWORD));
        UserResponse boardAssignee = authService.signUp(new SignUpRequest("Board Assignee E2E", BOARD_ASSIGNEE_EMAIL, SEED_PASSWORD));

        actingAs(ADMIN_EMAIL, () -> {
            ProjectResponse project = projectService.create(new CreateProjectRequest(PROJECT_NAME, null));
            projectMemberService.add(project.id(), new AddProjectMembersRequest(List.of(assignee.id())));

            taskService.create(project.id(), new CreateTaskRequest(TASK_TITLE, null, TaskStatus.TODO, Priority.MEDIUM, assignee.id(), null));

            ProjectResponse boardProject = projectService.create(new CreateProjectRequest(BOARD_PROJECT_NAME, null));
            projectMemberService.add(boardProject.id(), new AddProjectMembersRequest(List.of(boardAssignee.id())));

            taskService.create(boardProject.id(), new CreateTaskRequest(BOARD_DRAG_TASK_TITLE, null, TaskStatus.TODO, Priority.MEDIUM, null, null));

            for (int i = 1; i <= WIP_LIMIT; i++) {
                taskService.create(boardProject.id(), new CreateTaskRequest(BOARD_WIP_TASK_TITLE_PREFIX + i, null, TaskStatus.IN_PROGRESS, Priority.MEDIUM, boardAssignee.id(), null));
            }

            taskService.create(boardProject.id(), new CreateTaskRequest(BOARD_WIP_TARGET_TASK_TITLE, null, TaskStatus.TODO, Priority.MEDIUM, boardAssignee.id(), null));
        });

        log.info("[E2E-SEED]: admin={}, assignee={}, projeto='{}', task='{}'", ADMIN_EMAIL, ASSIGNEE_EMAIL, PROJECT_NAME, TASK_TITLE);
        log.info("[E2E-SEED]: boardAssignee={}, projeto='{}'", BOARD_ASSIGNEE_EMAIL, BOARD_PROJECT_NAME);
    }

    private void actingAs(String email, Runnable action) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of()));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
