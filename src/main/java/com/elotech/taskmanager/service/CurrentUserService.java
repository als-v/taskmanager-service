package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.domain.error.UnauthorizedException;
import com.elotech.taskmanager.repository.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException(ErrorMessages.AUTH_UNAUTHENTICATED_CODE, ErrorMessages.AUTH_UNAUTHENTICATED_MESSAGE);
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException(ErrorMessages.AUTH_UNAUTHENTICATED_CODE, ErrorMessages.AUTH_USER_NOT_FOUND_MESSAGE));
    }
}
