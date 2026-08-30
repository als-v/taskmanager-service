package com.elotech.taskmanager.repository.task;

import com.elotech.taskmanager.domain.criteria.TaskListCriteria;
import com.elotech.taskmanager.domain.entity.Task;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TaskSpecifications {

    private static final String USER_ID = "userId";
    private static final char LIKE_ESCAPE = (char) 92;

    private TaskSpecifications() {
    }

    public static Specification<Task> byCriteria(UUID projectId, TaskListCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("projectId"), projectId));
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

            addFieldFilters(predicates, root, criteriaBuilder, criteria);
            addRangeFilter(predicates, root, criteriaBuilder, criteria);
            addSearchFilter(predicates, root, criteriaBuilder, criteria.q());

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addFieldFilters(List<Predicate> predicates, Root<Task> root,
                                        CriteriaBuilder criteriaBuilder, TaskListCriteria criteria) {
        if (criteria.status() != null) {
            predicates.add(criteriaBuilder.equal(root.get("status"), criteria.status()));
        }
        if (criteria.priority() != null) {
            predicates.add(criteriaBuilder.equal(root.get("priority"), criteria.priority()));
        }
        if (criteria.unassigned() != null && criteria.unassigned()) {
            predicates.add(criteriaBuilder.isNull(root.get(USER_ID)));
        }
        if (criteria.unassigned() != null && !criteria.unassigned()) {
            predicates.add(criteriaBuilder.isNotNull(root.get(USER_ID)));
        }
        if (criteria.assigneeId() != null) {
            predicates.add(criteriaBuilder.equal(root.get(USER_ID), criteria.assigneeId()));
        }
    }

    private static void addRangeFilter(List<Predicate> predicates, Root<Task> root,
                                       CriteriaBuilder criteriaBuilder, TaskListCriteria criteria) {
        if (criteria.dueDateFrom() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), criteria.dueDateFrom()));
        }
        if (criteria.dueDateTo() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), criteria.dueDateTo()));
        }
    }

    private static void addSearchFilter(List<Predicate> predicates, Root<Task> root,
                                        CriteriaBuilder criteriaBuilder, String q) {
        if (q == null) {
            return;
        }

        String pattern = "%" + escapeLike(q.toLowerCase(Locale.ROOT)) + "%";
        predicates.add(criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern, LIKE_ESCAPE),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern, LIKE_ESCAPE)
        ));
    }

    private static String escapeLike(String value) {
        String escape = String.valueOf(LIKE_ESCAPE);
        return value
                .replace(escape, escape + escape)
                .replace("%", escape + "%")
                .replace("_", escape + "_");
    }
}
