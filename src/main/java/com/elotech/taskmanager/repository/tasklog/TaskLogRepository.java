package com.elotech.taskmanager.repository.tasklog;

import com.elotech.taskmanager.domain.entity.TaskLog;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TaskLogRepository extends JpaRepository<TaskLog, UUID> {

    @Query("""
            select new com.elotech.taskmanager.repository.tasklog.TaskLogWithReferences(tl, t, u)
            from TaskLog tl
            join Task t on t.id = tl.taskId
            join User u on u.id = tl.actorId
            where tl.projectId = :projectId
              and (:taskId is null or tl.taskId = :taskId)
              and (:actorId is null or tl.actorId = :actorId)
              and (:action is null or tl.action = :action)
              and (:createdAtFrom is null or tl.createdAt >= :createdAtFrom)
              and (:createdAtTo is null or tl.createdAt <= :createdAtTo)
            """)
    Page<TaskLogWithReferences> findByProjectIdWithFilters(
            @Param("projectId") UUID projectId,
            @Param("taskId") UUID taskId,
            @Param("actorId") UUID actorId,
            @Param("action") AuditAction action,
            @Param("createdAtFrom") LocalDateTime createdAtFrom,
            @Param("createdAtTo") LocalDateTime createdAtTo,
            Pageable pageable
    );
}
