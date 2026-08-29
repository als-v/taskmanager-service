package com.elotech.taskmanager.repository.task;

import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.criteria.TaskListCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TaskSpecifications {

    private static final String USER_ID = "userId";

    private TaskSpecifications() { }

    public static Specification<Task> byCriteria(UUID projectId, TaskListCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("projectId"), projectId));
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

            addFieldFilters(predicates, root, criteriaBuilder, criteria);
            addRangeFilter(predicates, root, criteriaBuilder, criteria);
            addTextFilter(predicates, root, criteriaBuilder, criteria.title(), "title");
            addTextFilter(predicates, root, criteriaBuilder, criteria.description(), "description");

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static void addFieldFilters(List<jakarta.persistence.criteria.Predicate> predicates, jakarta.persistence.criteria.Root<Task> root, jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, TaskListCriteria criteria) {
        if (criteria.status() != null) predicates.add(criteriaBuilder.equal(root.get("status"), criteria.status()));
        if (criteria.priority() != null) predicates.add(criteriaBuilder.equal(root.get("priority"), criteria.priority()));

        if (criteria.unassigned() != null && criteria.unassigned()) {
            predicates.add(criteriaBuilder.isNull(root.get(USER_ID)));
        } else if (criteria.unassigned() != null) {
            predicates.add(criteriaBuilder.isNotNull(root.get(USER_ID)));
        } else if (criteria.assigneeId() != null) {
            predicates.add(criteriaBuilder.equal(root.get(USER_ID), criteria.assigneeId()));
        }
    }

    private static void addRangeFilter(List<jakarta.persistence.criteria.Predicate> predicates, jakarta.persistence.criteria.Root<Task> root, jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, TaskListCriteria criteria) {
        if (criteria.dueDateFrom() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), criteria.dueDateFrom()));
        }

        if (criteria.dueDateTo() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), criteria.dueDateTo()));
        }
    }

    private static void addTextFilter(List<jakarta.persistence.criteria.Predicate> predicates, jakarta.persistence.criteria.Root<Task> root, jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, String value, String field) {
        if (value != null) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), likePattern(value)));
        }
    }

    private static String likePattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }
}
