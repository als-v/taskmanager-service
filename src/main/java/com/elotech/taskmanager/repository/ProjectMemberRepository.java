package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.entity.ProjectMember;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query("""
            select pm.role
            from ProjectMember pm
            where pm.projectId = :projectId
              and pm.userId = :userId
            """)
    Optional<MemberRole> findRoleByProjectIdAndUserId(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
