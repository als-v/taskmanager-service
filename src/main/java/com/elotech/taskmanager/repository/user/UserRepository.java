package com.elotech.taskmanager.repository.user;

import com.elotech.taskmanager.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @Query("""
            select u
            from User u
            where (:name is null or lower(u.name) like lower(concat('%', cast(:name as string), '%')))
              and (:email is null or lower(u.email) like lower(concat('%', cast(:email as string), '%')))
            """)
    Page<User> findByFilters(@Param("name") String name, @Param("email") String email, Pageable pageable);

    @Query("""
            select u
            from User u
            where (:name is null or lower(u.name) like lower(concat('%', cast(:name as string), '%')))
              and (:email is null or lower(u.email) like lower(concat('%', cast(:email as string), '%')))
              and not exists (
                  select pm.id
                  from ProjectMember pm
                  where pm.projectId = :projectId
                    and pm.userId = u.id
              )
            """)
    Page<User> findAvailableForProject(
            @Param("projectId") UUID projectId,
            @Param("name") String name,
            @Param("email") String email,
            Pageable pageable
    );
}
