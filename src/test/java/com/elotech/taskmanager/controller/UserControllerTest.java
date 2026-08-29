package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.response.auth.UserResponse;
import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void listUsersPassesExplicitFiltersAndSerializesPage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UserResponse user = new UserResponse(userId, "Ana Silva", "ana@example.com");
        given(userService.list(0, 20, "Ana", "example", projectId))
                .willReturn(new PageResponse<>(List.of(user), 0, 20, 1, 1));

        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "20")
                        .param("name", "Ana")
                        .param("email", "example")
                        .param("project_id", projectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Ana Silva"))
                .andExpect(jsonPath("$.content[0].email").value("ana@example.com"))
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.total_pages").value(1));

        verify(userService).list(0, 20, "Ana", "example", projectId);
    }

    @Test
    void listUsersReturnsBadRequestForInvalidPagination() throws Exception {
        given(userService.list(-1, 20, null, null, null))
                .willThrow(new BadRequestException(ErrorMessages.PAGINATION_PAGE_INVALID_CODE, ErrorMessages.PAGINATION_PAGE_INVALID_MESSAGE));

        mockMvc.perform(get("/api/users").param("page", "-1").param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorMessages.PAGINATION_PAGE_INVALID_CODE));
    }
}
