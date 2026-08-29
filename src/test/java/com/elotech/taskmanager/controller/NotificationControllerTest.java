package com.elotech.taskmanager.controller;

import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.notification.NotificationResponse;
import com.elotech.taskmanager.domain.enumeration.NotificationType;
import com.elotech.taskmanager.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private NotificationService notificationService;

    @Test
    void serializesNotificationListAsSnakeCase() throws Exception {
        NotificationResponse notification = notification(null);
        given(notificationService.list(true, 0, 20))
                .willReturn(new PageResponse<>(List.of(notification), 0, 20, 1, 1));

        mockMvc.perform(get("/api/notifications")
                        .param("unread", "true")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(notification.id().toString()))
                .andExpect(jsonPath("$.content[0].project_id").value(notification.projectId().toString()))
                .andExpect(jsonPath("$.content[0].task_id").value(notification.taskId().toString()))
                .andExpect(jsonPath("$.content[0].created_by").value(notification.createdBy().toString()))
                .andExpect(jsonPath("$.content[0].read_at").doesNotExist())
                .andExpect(jsonPath("$.content[0].unread").value(true))
                .andExpect(jsonPath("$.total_pages").value(1));

        verify(notificationService).list(true, 0, 20);
    }

    @Test
    void marksNotificationAsRead() throws Exception {
        UUID notificationId = UUID.randomUUID();
        LocalDateTime readAt = LocalDateTime.of(2026, 1, 10, 9, 30);
        NotificationResponse notification = notification(readAt);
        given(notificationService.markAsRead(notificationId)).willReturn(notification);

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.id().toString()))
                .andExpect(jsonPath("$.read_at").value("2026-01-10T09:30:00"))
                .andExpect(jsonPath("$.unread").value(false));

        verify(notificationService).markAsRead(notificationId);
    }

    @Test
    void returnsBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.request.parameter-invalid"));
    }

    private NotificationResponse notification(LocalDateTime readAt) {
        return new NotificationResponse(
                UUID.randomUUID(),
                NotificationType.TASK_ASSIGNED,
                "Voce foi atribuido a uma task",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.of(2026, 1, 10, 9, 0),
                readAt,
                readAt == null
        );
    }
}
