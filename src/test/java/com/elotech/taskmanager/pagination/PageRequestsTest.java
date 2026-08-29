package com.elotech.taskmanager.pagination;

import com.elotech.taskmanager.domain.error.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestsTest {

    @Test
    void shouldApplyDefaultPageAndSize() {
        PageRequest pageRequest = PageRequests.of(null, null);

        assertThat(pageRequest.getPageNumber()).isEqualTo(PageRequests.DEFAULT_PAGE);
        assertThat(pageRequest.getPageSize()).isEqualTo(PageRequests.DEFAULT_SIZE);
        assertThat(pageRequest.getSort().isUnsorted()).isTrue();
    }

    @Test
    void shouldClampSizeToMaxSize() {
        PageRequest pageRequest = PageRequests.of(2, 200);

        assertThat(pageRequest.getPageNumber()).isEqualTo(2);
        assertThat(pageRequest.getPageSize()).isEqualTo(PageRequests.MAX_SIZE);
    }

    @Test
    void shouldRejectNegativePage() {
        assertThatThrownBy(() -> PageRequests.of(-1, 20))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectSizeLowerThanOne() {
        assertThatThrownBy(() -> PageRequests.of(0, 0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldApplyFixedSort() {
        PageRequest pageRequest = PageRequests.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        assertThat(pageRequest.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void shouldUseDefaultSortWhenSortByIsBlank() {
        PageRequest pageRequest = PageRequests.of(
                0,
                20,
                " ",
                "ASC",
                Set.of("name"),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        assertThat(pageRequest.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void shouldApplyDynamicSortWhenFieldIsAllowed() {
        PageRequest pageRequest = PageRequests.of(
                0,
                20,
                "name",
                "DESC",
                Set.of("name", "email"),
                Sort.by(Sort.Direction.ASC, "name")
        );

        assertThat(pageRequest.getSort().getOrderFor("name").isDescending()).isTrue();
    }

    @Test
    void shouldUseDefaultSortDirectionWhenDirectionIsBlank() {
        PageRequest pageRequest = PageRequests.of(
                0,
                20,
                "email",
                " ",
                Set.of("name", "email"),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        assertThat(pageRequest.getSort().getOrderFor("email").isDescending()).isTrue();
    }

    @Test
    void shouldRejectDynamicSortFieldOutsideWhitelist() {
        assertThatThrownBy(() -> PageRequests.of(
                0,
                20,
                "password",
                "ASC",
                Set.of("name", "email"),
                Sort.by(Sort.Direction.ASC, "name")
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectInvalidSortDirection() {
        assertThatThrownBy(() -> PageRequests.of(
                0,
                20,
                "name",
                "INVALID",
                Set.of("name", "email"),
                Sort.by(Sort.Direction.ASC, "name")
        )).isInstanceOf(BadRequestException.class);
    }
}
