package com.elotech.taskmanager.service;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CacheableEndpointTest {

    @Test
    void dashboardSummaryIsCachedByCurrentUserAndSelectedProject() throws NoSuchMethodException {
        Cacheable cacheable = cacheableAnnotation(DashboardService.class, "getDashboard", UUID.class);

        assertThat(cacheable.value()).containsExactly(CacheInvalidationService.DASHBOARD_SUMMARY_CACHE);
        assertThat(cacheable.key())
                .contains("@currentUserService.getCurrentUser().getId().toString()")
                .contains("#projectId");
    }

    @Test
    void dashboardWipIsCachedByCurrentUserAndSelectedProject() throws NoSuchMethodException {
        Cacheable cacheable = cacheableAnnotation(DashboardService.class, "getWip", UUID.class);

        assertThat(cacheable.value()).containsExactly(CacheInvalidationService.DASHBOARD_WIP_CACHE);
        assertThat(cacheable.key())
                .contains("@currentUserService.getCurrentUser().getId().toString()")
                .contains("#projectId");
    }

    @Test
    void projectMembersListIsCachedByProjectAndFilters() throws NoSuchMethodException {
        Cacheable cacheable = cacheableAnnotation(
                ProjectMemberService.class,
                "list",
                UUID.class,
                Integer.class,
                Integer.class,
                String.class,
                String.class
        );

        assertThat(cacheable.value()).containsExactly(CacheInvalidationService.PROJECT_MEMBERS_LIST_CACHE);
        assertThat(cacheable.key())
                .contains("@currentUserService.getCurrentUser().getId().toString()")
                .contains("#projectId")
                .contains("#page")
                .contains("#size")
                .contains("#name")
                .contains("#email");
    }

    @Test
    void userProjectsListIsCachedByCurrentUserAndFilters() throws NoSuchMethodException {
        Cacheable cacheable = cacheableAnnotation(
                ProjectService.class,
                "list",
                String.class,
                String.class,
                String.class,
                Integer.class,
                Integer.class
        );

        assertThat(cacheable.value()).containsExactly(CacheInvalidationService.USER_PROJECTS_LIST_CACHE);
        assertThat(cacheable.key())
                .contains("@currentUserService.getCurrentUser().getId().toString()")
                .contains("#page")
                .contains("#size")
                .contains("#sort")
                .contains("#name")
                .contains("#description");
    }

    private Cacheable cacheableAnnotation(Class<?> type, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = type.getMethod(methodName, parameterTypes);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertThat(cacheable).isNotNull();
        return cacheable;
    }
}
