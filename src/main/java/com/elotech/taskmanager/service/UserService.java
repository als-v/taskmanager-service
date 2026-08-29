package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.auth.UserResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.pagination.PageRequests;
import org.springframework.data.domain.PageRequest;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ProjectAccessPolicy projectAccessPolicy;

    public UserService(UserRepository userRepository, CurrentUserService currentUserService, ProjectAccessPolicy projectAccessPolicy) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.projectAccessPolicy = projectAccessPolicy;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Integer page, Integer size, String name, String email, UUID projectId) {
        User currentUser = currentUserService.getCurrentUser();
        PageRequest pageRequest = PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "name", "email"));

        if (projectId != null) {
            projectAccessPolicy.requireMember(projectId, currentUser.getId());
            return PageResponse.from(userRepository.findAvailableForProject(projectId, blankToNull(name), blankToNull(email), pageRequest)
                    .map(UserResponse::from));
        }

        return PageResponse.from(userRepository.findByFilters(blankToNull(name), blankToNull(email), pageRequest)
                .map(UserResponse::from));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
