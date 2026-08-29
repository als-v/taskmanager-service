package com.elotech.taskmanager.pagination;

import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageRequests {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static PageRequest of(Integer page, Integer size) {
        return PageRequest.of(pageValue(page), sizeValue(size));
    }

    public static PageRequest of(Integer page, Integer size, Sort sort) {
        return PageRequest.of(pageValue(page), sizeValue(size), sort);
    }

    public static PageRequest of(
            Integer page,
            Integer size,
            String sortBy,
            String direction,
            Set<String> allowedFields,
            Sort defaultSort
    ) {
        if (sortBy == null || sortBy.isBlank()) {
            return of(page, size, defaultSort);
        }

        String normalizedSortBy = sortBy.trim();
        if (allowedFields == null || !allowedFields.contains(normalizedSortBy)) {
            throw new BadRequestException(
                    ErrorMessages.REQUEST_PARAMETER_INVALID_CODE,
                    "Sort field is not allowed"
            );
        }

        return of(page, size, Sort.by(directionValue(direction, defaultSort), normalizedSortBy));
    }

    private static int pageValue(Integer page) {
        int pageValue = page == null ? DEFAULT_PAGE : page;
        if (pageValue < 0) {
            throw new BadRequestException(
                    ErrorMessages.PAGINATION_PAGE_INVALID_CODE,
                    ErrorMessages.PAGINATION_PAGE_INVALID_MESSAGE
            );
        }
        return pageValue;
    }

    private static int sizeValue(Integer size) {
        int sizeValue = size == null ? DEFAULT_SIZE : size;
        if (sizeValue < 1) {
            throw new BadRequestException(
                    ErrorMessages.PAGINATION_SIZE_INVALID_CODE,
                    ErrorMessages.PAGINATION_SIZE_INVALID_MESSAGE
            );
        }
        return Math.min(sizeValue, MAX_SIZE);
    }

    private static Sort.Direction directionValue(String direction, Sort defaultSort) {
        if (direction == null || direction.isBlank()) {
            return defaultDirection(defaultSort);
        }

        return Sort.Direction.fromOptionalString(direction.trim())
                .orElseThrow(() -> new BadRequestException(
                        ErrorMessages.REQUEST_PARAMETER_INVALID_CODE,
                        "Sort direction must be ASC or DESC"
                ));
    }

    private static Sort.Direction defaultDirection(Sort defaultSort) {
        if (defaultSort == null || defaultSort.isUnsorted()) {
            return Sort.Direction.ASC;
        }
        return defaultSort.iterator().next().getDirection();
    }
}
