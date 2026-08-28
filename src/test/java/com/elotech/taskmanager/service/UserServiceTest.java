package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.auth.UserResponse;
import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.policy.ProjectAccessPolicy;
import com.elotech.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProjectAccessPolicy projectAccessPolicy;

    private UserService userService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, currentUserService, projectAccessPolicy);
        currentUser = user("Maria", "maria@example.com");
    }

    @Test
    void listUsesDefaultsAndCapsSize() {
        User ana = user("Ana", "ana@example.com");
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(userRepository.findByFilters(eq(null), eq(null), any(Pageable.class))).willReturn(new PageImpl<>(List.of(ana)));

        PageResponse<UserResponse> response = userService.list(null, 200, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findByFilters(eq(null), eq(null), pageableCaptor.capture());
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).email()).isEqualTo("ana@example.com");
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("name").isAscending()).isTrue();
    }

    @Test
    void listFiltersByNameAndEmailWithAndSemanticsDelegatedToRepository() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(userRepository.findByFilters(eq("ana"), eq("dev"), any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        userService.list(0, 20, " ana ", "dev", null);

        verify(userRepository).findByFilters(eq("ana"), eq("dev"), any(Pageable.class));
    }

    @Test
    void listWithProjectValidatesAccessAndExcludesExistingMembers() {
        UUID projectId = UUID.randomUUID();
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(projectAccessPolicy.requireMember(projectId, currentUser.getId())).willReturn(Project.builder().id(projectId).build());
        given(userRepository.findAvailableForProject(eq(projectId), eq("joao"), eq(null), any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        userService.list(0, 20, "joao", null, projectId);

        verify(projectAccessPolicy).requireMember(projectId, currentUser.getId());
        verify(userRepository).findAvailableForProject(eq(projectId), eq("joao"), eq(null), any(Pageable.class));
    }

    @Test
    void listRejectsInvalidPagination() {
        assertThatThrownBy(() -> userService.list(-1, 20, null, null, null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> userService.list(0, 0, null, null, null)).isInstanceOf(BadRequestException.class);
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
