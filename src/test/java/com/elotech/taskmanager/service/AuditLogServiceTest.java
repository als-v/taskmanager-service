package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.audit.AuditLogResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.entity.TaskLog;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.tasklog.TaskLogRepository;
import com.elotech.taskmanager.repository.tasklog.TaskLogWithReferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private TaskLogRepository taskLogRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ProjectAccessPolicy projectAccessPolicy;

    private AuditLogService auditLogService;
    private User currentUser;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(taskLogRepository, currentUserService, projectAccessPolicy);
        currentUser = User.builder().id(UUID.randomUUID()).name("Maria").email("maria@example.com").build();
        projectId = UUID.randomUUID();
    }

    @Test
    void listsLogsWithTaskAndActorNames() {
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        TaskLog log = log(projectId, taskId, actorId, AuditAction.STATUS_CHANGED);
        Task task = Task.builder().id(taskId).projectId(projectId).title("Criar endpoint de status").status(TaskStatus.TODO).priority(Priority.HIGH).build();
        User actor = User.builder().id(actorId).name("Joao Silva").email("joao@example.com").build();
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(Project.builder().id(projectId).build());
        given(taskLogRepository.findByProjectIdWithFilters(eq(projectId), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(new TaskLogWithReferences(log, task, actor))));

        PageResponse<AuditLogResponse> response = auditLogService.list(projectId, null, null, null, null, null, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(log.getId());
        assertThat(response.content().get(0).projectId()).isEqualTo(projectId);
        assertThat(response.content().get(0).task().id()).isEqualTo(taskId);
        assertThat(response.content().get(0).task().name()).isEqualTo("Criar endpoint de status");
        assertThat(response.content().get(0).actor().id()).isEqualTo(actorId);
        assertThat(response.content().get(0).actor().name()).isEqualTo("Joao Silva");
        verify(projectAccessPolicy).requireMember(projectId, currentUser.getId());
    }

    @Test
    void appliesFiltersDateRangePaginationAndSizeLimit() {
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 1, 10, 8, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 10, 18, 0);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(Project.builder().id(projectId).build());
        given(taskLogRepository.findByProjectIdWithFilters(eq(projectId), eq(taskId), eq(actorId), eq(AuditAction.TASK_UPDATED), eq(from), eq(to), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        auditLogService.list(projectId, taskId, actorId, AuditAction.TASK_UPDATED, from, to, 2, 200);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskLogRepository).findByProjectIdWithFilters(
                eq(projectId),
                eq(taskId),
                eq(actorId),
                eq(AuditAction.TASK_UPDATED),
                eq(from),
                eq(to),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void rejectsInvalidPagination() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(Project.builder().id(projectId).build());

        assertThatThrownBy(() -> auditLogService.list(projectId, null, null, null, null, null, -1, 20))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> auditLogService.list(projectId, null, null, null, null, null, 0, 0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsCreatedAtFromAfterCreatedAtTo() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 11, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 10, 0, 0);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(Project.builder().id(projectId).build());

        assertThatThrownBy(() -> auditLogService.list(projectId, null, null, null, from, to, 0, 20))
                .isInstanceOfSatisfying(BadRequestException.class, exception -> assertThat(exception.getCode())
                        .isEqualTo(ErrorMessages.REQUEST_PARAMETER_INVALID_CODE));
    }

    @Test
    void propagatesProjectAccessFailureBeforeQuery() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId()))
                .willThrow(new NotFoundException(ErrorMessages.PROJECT_NOT_FOUND_CODE, ErrorMessages.PROJECT_NOT_FOUND_MESSAGE));

        assertThatThrownBy(() -> auditLogService.list(projectId, null, null, null, null, null, 0, 20))
                .isInstanceOf(NotFoundException.class);
    }

    private TaskLog log(UUID projectId, UUID taskId, UUID actorId, AuditAction action) {
        return TaskLog.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .taskId(taskId)
                .actorId(actorId)
                .action(action)
                .fromStatus(TaskStatus.TODO)
                .toStatus(TaskStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.of(2026, 1, 10, 9, 0))
                .build();
    }
}
