package com.taskmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.api.dto.TaskRequestDto;
import com.taskmanagement.api.dto.TaskResponseDto;
import com.taskmanagement.api.entity.Task;
import com.taskmanagement.api.entity.TaskStatus;
import com.taskmanagement.api.model.User;
import com.taskmanagement.api.repository.TaskRepository;
import com.taskmanagement.api.repository.UserRepository;
import com.taskmanagement.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String authToken;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User testUser = new User("integrationuser", passwordEncoder.encode("password123"), Set.of("ROLE_USER"));
        userRepository.save(testUser);

        authToken = "Bearer " + jwtService.generateToken("integrationuser");
    }

    @Test
    @DisplayName("Unauthenticated request to /tasks should be rejected with 401 Unauthorized")
    void unauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("End-to-end CRUD flow: Create, Read, Update, Delete tasks with JWT authentication")
    void fullCrudIntegrationFlow() throws Exception {
        // 1. Create a task (POST /tasks) -> 201 Created
        TaskRequestDto createRequest = TaskRequestDto.builder()
                .title("Integration Test Task")
                .description("Testing end to end flow")
                .status(TaskStatus.PENDING)
                .dueDate(Instant.parse("2026-12-31T23:59:59Z"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title", is("Integration Test Task")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        TaskResponseDto createdTask = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                TaskResponseDto.class
        );
        Long taskId = createdTask.getId();

        // 2. Read all tasks (GET /tasks) -> 200 OK
        mockMvc.perform(get("/tasks")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(taskId.intValue())))
                .andExpect(jsonPath("$[0].title", is("Integration Test Task")));

        // 3. Read task by ID (GET /tasks/{id}) -> 200 OK
        mockMvc.perform(get("/tasks/" + taskId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskId.intValue())))
                .andExpect(jsonPath("$.title", is("Integration Test Task")))
                .andExpect(jsonPath("$.description", is("Testing end to end flow")));

        // 4. Update task (PUT /tasks/{id}) -> 200 OK
        TaskRequestDto updateRequest = TaskRequestDto.builder()
                .title("Updated Integration Task")
                .description("Updated description")
                .status(TaskStatus.COMPLETED)
                .dueDate(Instant.parse("2026-11-30T18:00:00Z"))
                .build();

        mockMvc.perform(put("/tasks/" + taskId)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskId.intValue())))
                .andExpect(jsonPath("$.title", is("Updated Integration Task")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        // Verify entity persisted in DB
        Task persisted = taskRepository.findById(taskId).orElseThrow();
        assertThat(persisted.getTitle()).isEqualTo("Updated Integration Task");
        assertThat(persisted.getStatus()).isEqualTo(TaskStatus.COMPLETED);

        // 5. Delete task (DELETE /tasks/{id}) -> 204 No Content
        mockMvc.perform(delete("/tasks/" + taskId)
                        .header("Authorization", authToken))
                .andExpect(status().isNoContent());

        // Verify entity deleted from DB
        assertThat(taskRepository.existsById(taskId)).isFalse();

        // 6. Confirm 404 for deleted task (GET /tasks/{id}) -> 404 Not Found
        mockMvc.perform(get("/tasks/" + taskId)
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Task not found with id: " + taskId)));
    }

    @Test
    @DisplayName("GET /tasks/{id} returns 404 when task does not exist")
    void getMissingTask_Returns404() throws Exception {
        mockMvc.perform(get("/tasks/99999")
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Task not found with id: 99999")));
    }

    @Test
    @DisplayName("PUT /tasks/{id} returns 404 when updating non-existent task")
    void updateMissingTask_Returns404() throws Exception {
        TaskRequestDto updateRequest = TaskRequestDto.builder()
                .title("Non-existent task")
                .build();

        mockMvc.perform(put("/tasks/99999")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("DELETE /tasks/{id} returns 404 when deleting non-existent task")
    void deleteMissingTask_Returns404() throws Exception {
        mockMvc.perform(delete("/tasks/99999")
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("POST /tasks returns 400 when title is blank")
    void createTaskWithBlankTitle_Returns400() throws Exception {
        TaskRequestDto invalidRequest = TaskRequestDto.builder()
                .title("")
                .build();

        mockMvc.perform(post("/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.title", is("Title cannot be blank")));
    }

    @Test
    @DisplayName("POST /tasks returns 400 when due date is in the past")
    void createTaskWithPastDueDate_Returns400() throws Exception {
        Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
        TaskRequestDto invalidRequest = TaskRequestDto.builder()
                .title("Valid Title")
                .dueDate(pastDate)
                .build();

        mockMvc.perform(post("/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.validationErrors.dueDate",
                        is("Due date must be today or in the future")));
    }

    @Test
    @DisplayName("POST /tasks returns 400 when title exceeds 200 characters")
    void createTaskWithTitleTooLong_Returns400() throws Exception {
        TaskRequestDto invalidRequest = TaskRequestDto.builder()
                .title("X".repeat(201))
                .build();

        mockMvc.perform(post("/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.validationErrors.title",
                        is("Title must not exceed 200 characters")));
    }
}
