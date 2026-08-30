package com.elotech.taskmanager.repository.task;

import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    Page<Task> findAllByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);

    long countByProjectIdAndUserIdAndStatusAndDeletedAtIsNull(UUID projectId, UUID userId, TaskStatus status);

    @Modifying
    @Query("update Task t set t.deletedAt = :deletedAt where t.projectId = :projectId and t.deletedAt is null")
    int updateDeletedAtByProjectIdAndDeletedAtIsNull(UUID projectId, LocalDateTime deletedAt);

    @Query("""
            select t.status as status, count(t.id) as total
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.deletedAt is null
              and p.deletedAt is null
            group by t.status
            """)
    List<TaskStatusCountProjection> countByStatusForMemberProjects(@Param("userId") UUID userId);

    @Query("""
            select t.status as status, count(t.id) as total
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.projectId = :projectId
              and t.deletedAt is null
              and p.deletedAt is null
            group by t.status
            """)
    List<TaskStatusCountProjection> countByStatusForMemberProject(
            @Param("userId") UUID userId,
            @Param("projectId") UUID projectId
    );

    @Query("""
            select t.priority as priority, count(t.id) as total
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.deletedAt is null
              and p.deletedAt is null
            group by t.priority
            """)
    List<TaskPriorityCountProjection> countByPriorityForMemberProjects(@Param("userId") UUID userId);

    @Query("""
            select t.priority as priority, count(t.id) as total
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.projectId = :projectId
              and t.deletedAt is null
              and p.deletedAt is null
            group by t.priority
            """)
    List<TaskPriorityCountProjection> countByPriorityForMemberProject(
            @Param("userId") UUID userId,
            @Param("projectId") UUID projectId
    );

    @Query("""
            select count(t.id)
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.deletedAt is null
              and p.deletedAt is null
              and t.status <> :doneStatus
              and t.dueDate < :now
            """)
    long countOverdueForMemberProjects(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now,
            @Param("doneStatus") TaskStatus doneStatus
    );

    @Query("""
            select count(t.id)
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.projectId = :projectId
              and t.deletedAt is null
              and p.deletedAt is null
              and t.status <> :doneStatus
              and t.dueDate < :now
            """)
    long countOverdueForMemberProject(
            @Param("userId") UUID userId,
            @Param("projectId") UUID projectId,
            @Param("now") LocalDateTime now,
            @Param("doneStatus") TaskStatus doneStatus
    );

    @Query("""
            select count(t.id)
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.deletedAt is null
              and p.deletedAt is null
              and t.status <> :doneStatus
              and t.dueDate >= :now
              and t.dueDate <= :dueSoonLimit
            """)
    long countDueSoonForMemberProjects(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now,
            @Param("dueSoonLimit") LocalDateTime dueSoonLimit,
            @Param("doneStatus") TaskStatus doneStatus
    );

    @Query("""
            select count(t.id)
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.projectId = :projectId
              and t.deletedAt is null
              and p.deletedAt is null
              and t.status <> :doneStatus
              and t.dueDate >= :now
              and t.dueDate <= :dueSoonLimit
            """)
    long countDueSoonForMemberProject(
            @Param("userId") UUID userId,
            @Param("projectId") UUID projectId,
            @Param("now") LocalDateTime now,
            @Param("dueSoonLimit") LocalDateTime dueSoonLimit,
            @Param("doneStatus") TaskStatus doneStatus
    );

    @Query("""
            select u.id as userId, u.name as name, u.email as email, count(t.id) as inProgress
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            join User u on u.id = t.userId
            where pm.userId = :userId
              and t.deletedAt is null
              and p.deletedAt is null
              and t.status = :status
              and t.userId is not null
            group by u.id, u.name, u.email
            order by u.name asc, u.email asc
            """)
    List<TaskWipAssigneeProjection> countWipByAssigneeForMemberProjects(
            @Param("userId") UUID userId,
            @Param("status") TaskStatus status
    );

    @Query("""
            select u.id as userId, u.name as name, u.email as email, count(t.id) as inProgress
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            join User u on u.id = t.userId
            where pm.userId = :userId
              and t.projectId = :projectId
              and t.deletedAt is null
              and p.deletedAt is null
              and t.status = :status
              and t.userId is not null
            group by u.id, u.name, u.email
            order by u.name asc, u.email asc
            """)
    List<TaskWipAssigneeProjection> countWipByAssigneeForMemberProject(
            @Param("userId") UUID userId,
            @Param("projectId") UUID projectId,
            @Param("status") TaskStatus status
    );
}
