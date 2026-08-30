package com.elotech.taskmanager.service;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheInvalidationServiceTest {

    @Test
    void evictDashboardCachesClearsSummaryAndWipOnly() {
        ConcurrentMapCacheManager cacheManager = cacheManager();
        CacheInvalidationService service = new CacheInvalidationService(cacheManager);
        putInAllCaches(cacheManager);

        service.evictDashboardCaches();

        assertThat(cacheManager.getCache(CacheInvalidationService.DASHBOARD_SUMMARY_CACHE).get("key")).isNull();
        assertThat(cacheManager.getCache(CacheInvalidationService.DASHBOARD_WIP_CACHE).get("key")).isNull();
        assertThat(cacheManager.getCache(CacheInvalidationService.PROJECT_MEMBERS_LIST_CACHE).get("key")).isNotNull();
        assertThat(cacheManager.getCache(CacheInvalidationService.USER_PROJECTS_LIST_CACHE).get("key")).isNotNull();
    }

    @Test
    void evictProjectMutationCachesClearsProjectsAndDashboards() {
        ConcurrentMapCacheManager cacheManager = cacheManager();
        CacheInvalidationService service = new CacheInvalidationService(cacheManager);
        putInAllCaches(cacheManager);

        service.evictProjectMutationCaches();

        assertThat(cacheManager.getCache(CacheInvalidationService.DASHBOARD_SUMMARY_CACHE).get("key")).isNull();
        assertThat(cacheManager.getCache(CacheInvalidationService.DASHBOARD_WIP_CACHE).get("key")).isNull();
        assertThat(cacheManager.getCache(CacheInvalidationService.USER_PROJECTS_LIST_CACHE).get("key")).isNull();
        assertThat(cacheManager.getCache(CacheInvalidationService.PROJECT_MEMBERS_LIST_CACHE).get("key")).isNotNull();
    }

    @Test
    void evictMemberMutationCachesClearsAllCachedProjectMembershipViews() {
        ConcurrentMapCacheManager cacheManager = cacheManager();
        CacheInvalidationService service = new CacheInvalidationService(cacheManager);
        putInAllCaches(cacheManager);

        service.evictMemberMutationCaches();

        assertThat(cacheManager.getCache(CacheInvalidationService.DASHBOARD_SUMMARY_CACHE).get("key")).isNull();
        assertThat(cacheManager.getCache(CacheInvalidationService.DASHBOARD_WIP_CACHE).get("key")).isNull();
        assertThat(cacheManager.getCache(CacheInvalidationService.USER_PROJECTS_LIST_CACHE).get("key")).isNull();
        assertThat(cacheManager.getCache(CacheInvalidationService.PROJECT_MEMBERS_LIST_CACHE).get("key")).isNull();
    }

    private ConcurrentMapCacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CacheInvalidationService.DASHBOARD_SUMMARY_CACHE,
                CacheInvalidationService.DASHBOARD_WIP_CACHE,
                CacheInvalidationService.PROJECT_MEMBERS_LIST_CACHE,
                CacheInvalidationService.USER_PROJECTS_LIST_CACHE
        );
    }

    private void putInAllCaches(ConcurrentMapCacheManager cacheManager) {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            cache.put("key", "value");
        });
    }
}
