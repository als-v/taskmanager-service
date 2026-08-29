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
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.pagination.PageRequests;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.NotificationRepository;
import com.elotech.taskmanager.repository.ProjectMemberRepository;
import com.elotech.taskmanager.repository.UserNotificationRepository;
import com.elotech.taskmanager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAccessPolicy projectAccessPolicy;

    public ProjectMemberService(
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            UserNotificationRepository userNotificationRepository,
            CurrentUserService currentUserService,
            ProjectAccessPolicy projectAccessPolicy
    ) {
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.currentUserService = currentUserService;
        this.projectAccessPolicy = projectAccessPolicy;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectMemberResponse> list(UUID projectId, Integer page, Integer size, String name, String email) {
        User currentUser = currentUserService.getCurrentUser();
        projectAccessPolicy.requireMember(projectId, currentUser.getId());

        return PageResponse.from(projectMemberRepository.findMembersByProjectId(
                projectId,
                blankToNull(name),
                blankToNull(email),
                PageRequests.of(page, size)
        ));
    }

    @Transactional
    public List<ProjectMemberResponse> add(UUID projectId, AddProjectMembersRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Project project = projectAccessPolicy.requireAdmin(projectId, currentUser.getId());
        List<UUID> userIds = request == null ? null : request.userIds();

        validateUserIds(userIds);

        List<User> users = userRepository.findAllById(userIds);

        if (users.size() != userIds.size()) {
            throw new NotFoundException(ErrorMessages.USER_NOT_FOUND_CODE, ErrorMessages.USER_NOT_FOUND_MESSAGE);
        }

        List<UUID> existingUserIds = projectMemberRepository.findExistingUserIds(projectId, userIds);

        if (!existingUserIds.isEmpty()) {
            throw new ConflictException(ErrorMessages.PROJECT_MEMBER_ALREADY_EXISTS_CODE, ErrorMessages.PROJECT_MEMBER_ALREADY_EXISTS_MESSAGE);
        }

        Map<UUID, User> usersById = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<ProjectMember> members = userIds.stream()
                .map(userId -> ProjectMember.builder()
                        .projectId(projectId)
                        .userId(userId)
                        .role(MemberRole.MEMBER)
                        .build())
                .toList();

        List<ProjectMember> savedMembers = projectMemberRepository.saveAll(members);
        savedMembers.forEach(member -> notifyProjectAdded(project, currentUser.getId(), member.getUserId()));

        return savedMembers.stream()
                .map(member -> {
                    User user = usersById.get(member.getUserId());
                    return new ProjectMemberResponse(member.getUserId(), user.getName(), user.getEmail(), member.getRole(), member.getJoinedAt());
                })
                .toList();
    }

    private void validateUserIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BadRequestException(ErrorMessages.PROJECT_MEMBER_EMPTY_LIST_CODE, ErrorMessages.PROJECT_MEMBER_EMPTY_LIST_MESSAGE);
        }

        if (new HashSet<>(userIds).size() != userIds.size()) {
            throw new BadRequestException(ErrorMessages.PROJECT_MEMBER_DUPLICATE_USER_CODE, ErrorMessages.PROJECT_MEMBER_DUPLICATE_USER_MESSAGE);
        }
    }

    private void notifyProjectAdded(Project project, UUID actorId, UUID userId) {
        Notification notification = notificationRepository.save(Notification.builder()
                .type(NotificationType.PROJECT_ADDED)
                .message("Project added: " + project.getName())
                .projectId(project.getId())
                .taskId(null)
                .createdBy(actorId)
                .build());

        userNotificationRepository.save(UserNotification.builder()
                .id(UserNotification.Id.builder()
                        .userId(userId)
                        .notificationId(notification.getId())
                        .build())
                .readAt(null)
                .build());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
