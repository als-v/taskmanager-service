package com.elotech.taskmanager;

import com.elotech.taskmanager.domain.dto.request.auth.LoginRequest;
import com.elotech.taskmanager.domain.dto.request.auth.SignUpRequest;
import com.elotech.taskmanager.domain.dto.request.project.CreateProjectRequest;
import com.elotech.taskmanager.domain.dto.request.task.CreateTaskRequest;
import com.elotech.taskmanager.domain.dto.request.task.UpdateTaskStatusRequest;
import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String email = "usuario-" + UUID.randomUUID() + "@teste.com";
    private final String password = "123mudarA@";

    private String accessToken;
    private UUID userId;
    private UUID projectId;
    private UUID taskId;

    @Test
    @Order(1)
    void registroELoginRetornamTokenValido() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignUpRequest("Usuario", email, password))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        accessToken = body.get("access_token").asText();
        userId = UUID.fromString(body.get("user").get("id").asText());

        assertThat(accessToken).isNotBlank();
    }

    @Test
    @Order(2)
    void criacaoDeProjetoAutenticada() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest("Projeto #1", "Projeto criado pelo teste"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Projeto #1"))
                .andExpect(jsonPath("$.owner_id").value(userId.toString()))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        projectId = UUID.fromString(body.get("id").asText());
    }

    @Test
    @Order(3)
    void criacaoDeTaskAutenticada() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTaskRequest("Tarefa #1", "Descricao da tarefa", TaskStatus.TODO, Priority.MEDIUM, userId, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tarefa #1"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assignee.id").value(userId.toString()))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        taskId = UUID.fromString(body.get("id").asText());
    }

    @Test
    @Order(4)
    void limiteDeWipEBloqueadoNaQuintaTaskEmAndamento() throws Exception {
        mockMvc.perform(patch("/api/projects/" + projectId + "/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateTaskRequest("Tarefa # " + i, null, TaskStatus.IN_PROGRESS, Priority.MEDIUM, userId, null))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTaskRequest("Tarefa #6", null, TaskStatus.IN_PROGRESS, Priority.MEDIUM, userId, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("error.task.wip-limit-exceeded"));
    }

    @Test
    @Order(5)
    void passagemDeStatusDaTaskAteConclusao() throws Exception {
        mockMvc.perform(patch("/api/projects/" + projectId + "/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTaskStatusRequest(TaskStatus.DONE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @Order(6)
    void logsDeAuditoriaRegistramTodaAJornada() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/projects/" + projectId + "/logs")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("task_id", taskId.toString())
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        assertThat(content.isArray()).isTrue();

        boolean hasTaskCreated = false;
        boolean hasStatusChangedToInProgress = false;
        boolean hasStatusChangedToDone = false;

        for (JsonNode entry : content) {
            assertThat(entry.get("task").get("id").asText()).isEqualTo(taskId.toString());
            String action = entry.get("action").asText();

            if ("TASK_CREATED".equals(action)) {
                hasTaskCreated = true;
            } else if ("STATUS_CHANGED".equals(action)) {
                String toStatus = entry.get("to_status").asText();
                if ("IN_PROGRESS".equals(toStatus)) {
                    hasStatusChangedToInProgress = true;
                } else if ("DONE".equals(toStatus)) {
                    hasStatusChangedToDone = true;
                }
            }
        }

        assertThat(hasTaskCreated).as("log de criacao da task").isTrue();
        assertThat(hasStatusChangedToInProgress).as("log de mudanca para IN_PROGRESS").isTrue();
        assertThat(hasStatusChangedToDone).as("log de mudanca para DONE").isTrue();
    }
}
