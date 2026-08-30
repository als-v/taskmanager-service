package com.elotech.taskmanager.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CacheInvalidationService {

    public static final String DASHBOARD_SUMMARY_CACHE = "dashboard:summary";
    public static final String DASHBOARD_WIP_CACHE = "dashboard:wip";
    public static final String PROJECT_MEMBERS_LIST_CACHE = "project:members:list";
    public static final String USER_PROJECTS_LIST_CACHE = "user:projects:list";

    private final CacheManager cacheManager;

    public CacheInvalidationService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictDashboardCaches() {
        evictAll(DASHBOARD_SUMMARY_CACHE, DASHBOARD_WIP_CACHE);
    }

    public void evictProjectMutationCaches() {
        evictAll(USER_PROJECTS_LIST_CACHE, DASHBOARD_SUMMARY_CACHE, DASHBOARD_WIP_CACHE);
    }

    public void evictMemberMutationCaches() {
        evictAll(
                PROJECT_MEMBERS_LIST_CACHE,
                USER_PROJECTS_LIST_CACHE,
                DASHBOARD_SUMMARY_CACHE,
                DASHBOARD_WIP_CACHE
        );
    }

    private void evictAll(String... cacheNames) {
        List.of(cacheNames).forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }
}
