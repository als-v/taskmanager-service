package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.auth.UserResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

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
        PageRequest pageRequest = pageRequest(page, size);

        if (projectId != null) {
            projectAccessPolicy.requireMember(projectId, currentUser.getId());
            return PageResponse.from(userRepository.findAvailableForProject(projectId, blankToNull(name), blankToNull(email), pageRequest)
                    .map(UserResponse::from));
        }

        return PageResponse.from(userRepository.findByFilters(blankToNull(name), blankToNull(email), pageRequest)
                .map(UserResponse::from));
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        int pageValue = page == null ? DEFAULT_PAGE : page;
        int sizeValue = size == null ? DEFAULT_SIZE : size;

        if (pageValue < 0) {
            throw new BadRequestException(ErrorMessages.PAGINATION_PAGE_INVALID_CODE, ErrorMessages.PAGINATION_PAGE_INVALID_MESSAGE);
        }

        if (sizeValue < 1) {
            throw new BadRequestException(ErrorMessages.PAGINATION_SIZE_INVALID_CODE, ErrorMessages.PAGINATION_SIZE_INVALID_MESSAGE);
        }

        return PageRequest.of(pageValue, Math.min(sizeValue, MAX_SIZE), Sort.by(Sort.Direction.ASC, "name", "email"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
