package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.request.member.AddProjectMembersRequest;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.member.ProjectMemberResponse;
import com.elotech.taskmanager.domain.entity.Notification;
import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.ProjectMember;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.entity.UserNotification;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import com.elotech.taskmanager.domain.enumeration.NotificationType;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.ConflictException;
import com.elotech.taskmanager.domain.error.ForbiddenException;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.NotificationRepository;
import com.elotech.taskmanager.repository.ProjectMemberRepository;
import com.elotech.taskmanager.repository.UserNotificationRepository;
import com.elotech.taskmanager.repository.UserRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProjectAccessPolicy projectAccessPolicy;

    private ProjectMemberService projectMemberService;
    private User currentUser;
    private UUID projectId;
    private Project project;

    @BeforeEach
    void setUp() {
        projectMemberService = new ProjectMemberService(
                projectMemberRepository,
                userRepository,
                notificationRepository,
                userNotificationRepository,
                currentUserService,
                projectAccessPolicy
        );
        currentUser = user("Admin", "admin@example.com");
        projectId = UUID.randomUUID();
        project = Project.builder().id(projectId).name("Plataforma").ownerId(currentUser.getId()).build();
    }

    @Test
    void listMembersValidatesProjectMembershipAndFilters() {
        ProjectMemberResponse member = new ProjectMemberResponse(UUID.randomUUID(), "Ana", "ana@example.com", MemberRole.MEMBER, LocalDateTime.now());
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(project);
        given(projectMemberRepository.findMembersByProjectId(eq(projectId), eq("ana"), eq("dev"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(member)));

        PageResponse<ProjectMemberResponse> response = projectMemberService.list(projectId, 0, 20, " ana ", "dev");

        assertThat(response.content()).containsExactly(member);
        verify(projectAccessPolicy).requireMember(projectId, currentUser.getId());
    }

    @Test
    void addMultipleExistingUsersAsMembersAndCreatesNotifications() {
        User ana = user("Ana", "ana@example.com");
        User bruno = user("Bruno", "bruno@example.com");
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireAdmin(projectId, currentUser.getId())).willReturn(project);
        given(userRepository.findAllById(List.of(ana.getId(), bruno.getId()))).willReturn(List.of(ana, bruno));
        given(projectMemberRepository.findExistingUserIds(projectId, List.of(ana.getId(), bruno.getId()))).willReturn(List.of());
        given(projectMemberRepository.saveAll(any())).willAnswer(invocation -> {
            List<ProjectMember> members = invocation.getArgument(0);
            members.forEach(member -> {
                member.setId(UUID.randomUUID());
                member.setJoinedAt(LocalDateTime.of(2026, 1, 10, 9, 0));
            });
            return members;
        });
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            return notification;
        });

        List<ProjectMemberResponse> response = projectMemberService.add(projectId, new AddProjectMembersRequest(List.of(ana.getId(), bruno.getId())));

        assertThat(response).extracting(ProjectMemberResponse::userId).containsExactly(ana.getId(), bruno.getId());
        assertThat(response).extracting(ProjectMemberResponse::role).containsOnly(MemberRole.MEMBER);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getAllValues()).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.PROJECT_ADDED);
            assertThat(notification.getProjectId()).isEqualTo(projectId);
            assertThat(notification.getCreatedBy()).isEqualTo(currentUser.getId());
        });
        verify(userNotificationRepository, org.mockito.Mockito.times(2)).save(any(UserNotification.class));
    }

    @Test
    void addRejectsMemberActorWithForbidden() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireAdmin(projectId, currentUser.getId()))
                .willThrow(new ForbiddenException("error.project.admin-required", "Only project admins can perform this action"));

        AddProjectMembersRequest request = new AddProjectMembersRequest(List.of(UUID.randomUUID()));
        assertThatThrownBy(() -> projectMemberService.add(projectId, request))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(userRepository, notificationRepository, userNotificationRepository);
    }

    @Test
    void addRejectsEmptyListAndDuplicateUsers() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireAdmin(projectId, currentUser.getId())).willReturn(project);

        AddProjectMembersRequest emptyRequest = new AddProjectMembersRequest(List.of());
        assertThatThrownBy(() -> projectMemberService.add(projectId, emptyRequest))
                .isInstanceOf(BadRequestException.class);

        UUID userId = UUID.randomUUID();
        AddProjectMembersRequest duplicateRequest = new AddProjectMembersRequest(List.of(userId, userId));
        assertThatThrownBy(() -> projectMemberService.add(projectId, duplicateRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addRejectsMissingUserWithoutSavingAnything() {
        UUID userId = UUID.randomUUID();
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireAdmin(projectId, currentUser.getId())).willReturn(project);
        given(userRepository.findAllById(List.of(userId))).willReturn(List.of());

        AddProjectMembersRequest missingUserRequest = new AddProjectMembersRequest(List.of(userId));
        assertThatThrownBy(() -> projectMemberService.add(projectId, missingUserRequest))
                .isInstanceOf(NotFoundException.class);

        verify(projectMemberRepository, never()).saveAll(any());
        verifyNoInteractions(notificationRepository, userNotificationRepository);
    }

    @Test
    void addRejectsExistingMemberWithoutSavingAnything() {
        User ana = user("Ana", "ana@example.com");
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireAdmin(projectId, currentUser.getId())).willReturn(project);
        given(userRepository.findAllById(List.of(ana.getId()))).willReturn(List.of(ana));
        given(projectMemberRepository.findExistingUserIds(projectId, List.of(ana.getId()))).willReturn(List.of(ana.getId()));

        AddProjectMembersRequest existingMemberRequest = new AddProjectMembersRequest(List.of(ana.getId()));
        assertThatThrownBy(() -> projectMemberService.add(projectId, existingMemberRequest))
                .isInstanceOf(ConflictException.class);

        verify(projectMemberRepository, never()).saveAll(any());
        verifyNoInteractions(notificationRepository, userNotificationRepository);
    }

    private User user(String name, String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(email)
                .password("hash")
                .build();
    }
}
