package com.elotech.taskmanager.repository.project;

import com.elotech.taskmanager.domain.entity.Project;
import com.elotech.taskmanager.domain.entity.ProjectMember;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<Project> byMemberAndFilters(UUID userId, String name, String description) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            var member = query.from(ProjectMember.class);

            predicates.add(criteriaBuilder.equal(member.get("projectId"), root.get("id")));
            predicates.add(criteriaBuilder.equal(member.get("userId"), userId));
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            addTextFilter(predicates, root, criteriaBuilder, name, "name");
            addTextFilter(predicates, root, criteriaBuilder, description, "description");

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static void addTextFilter(
            List<jakarta.persistence.criteria.Predicate> predicates,
            jakarta.persistence.criteria.Root<Project> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            String value,
            String field
    ) {
        if (value == null) {
            return;
        }
        predicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(root.get(field)),
                "%" + value.toLowerCase(Locale.ROOT) + "%"
        ));
    }
}
