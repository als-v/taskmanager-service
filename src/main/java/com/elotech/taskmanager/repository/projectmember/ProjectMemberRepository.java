package com.elotech.taskmanager.repository.projectmember;

import com.elotech.taskmanager.domain.dto.response.member.ProjectMemberResponse;
import com.elotech.taskmanager.domain.entity.ProjectMember;
import com.elotech.taskmanager.domain.enumeration.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query("""
            select new com.elotech.taskmanager.domain.dto.response.member.ProjectMemberResponse(
                pm.userId,
                u.name,
                u.email,
                pm.role,
                pm.joinedAt
            )
            from ProjectMember pm
            join User u on u.id = pm.userId
            where pm.projectId = :projectId
              and (:name is null or lower(u.name) like lower(concat('%', cast(:name as string), '%')))
              and (:email is null or lower(u.email) like lower(concat('%', cast(:email as string), '%')))
            order by u.name asc, u.email asc
            """)
    Page<ProjectMemberResponse> findMembersByProjectId(
            @Param("projectId") UUID projectId,
            @Param("name") String name,
            @Param("email") String email,
            Pageable pageable
    );

    @Query("""
            select pm.userId
            from ProjectMember pm
            where pm.projectId = :projectId
              and pm.userId in :userIds
            """)
    List<UUID> findExistingUserIds(@Param("projectId") UUID projectId, @Param("userIds") List<UUID> userIds);
}
