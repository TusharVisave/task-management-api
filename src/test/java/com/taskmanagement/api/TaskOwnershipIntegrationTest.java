package com.taskmanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.api.dto.TaskRequestDto;
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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class TaskOwnershipIntegrationTest {

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

    @Autowired
    private com.taskmanagement.api.service.TaskService taskService;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private Task userBTask;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // Create User A
        userA = new User("userA", passwordEncoder.encode("PasswordA1!"), Set.of("ROLE_USER"));
        userA = userRepository.save(userA);
        tokenA = "Bearer " + jwtService.generateToken("userA");

        // Create User B
        userB = new User("userB", passwordEncoder.encode("PasswordB1!"), Set.of("ROLE_USER"));
        userB = userRepository.save(userB);
        tokenB = "Bearer " + jwtService.generateToken("userB");

        // Create a task owned by User B
        userBTask = Task.builder()
                .title("User B Private Task")
                .description("Sensitive data belonging to User B")
                .status(TaskStatus.PENDING)
                .createdAt(Instant.now())
                .owner(userB)
                .build();
        userBTask = taskRepository.save(userBTask);
    }

    @Test
    @DisplayName("Unauthenticated request to /tasks should be rejected with 401 Unauthorized")
    void unauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("User A cannot GET User B's task (403 Forbidden)")
    void userACannotGetUserBsTask_Returns403() throws Exception {
        mockMvc.perform(get("/tasks/" + userBTask.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("You do not have permission to access or modify this task")));
    }

    @Test
    @DisplayName("User A cannot PUT / modify User B's task (403 Forbidden)")
    void userACannotUpdateUserBsTask_Returns403() throws Exception {
        TaskRequestDto updateRequest = TaskRequestDto.builder()
                .title("User A Malicious Title")
                .description("Attempted overwrite")
                .status(TaskStatus.COMPLETED)
                .build();

        mockMvc.perform(put("/tasks/" + userBTask.getId())
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));

        // Verify task in DB remains unchanged
        Task taskAfterAttempt = taskRepository.findById(userBTask.getId()).orElseThrow();
        assertThat(taskAfterAttempt.getTitle()).isEqualTo("User B Private Task");
        assertThat(taskAfterAttempt.getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    @DisplayName("User A cannot DELETE User B's task (403 Forbidden)")
    void userACannotDeleteUserBsTask_Returns403() throws Exception {
        mockMvc.perform(delete("/tasks/" + userBTask.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));

        // Verify task still exists in DB
        assertThat(taskRepository.existsById(userBTask.getId())).isTrue();
    }

    @Test
    @DisplayName("GET /tasks filters by owner - User A only sees their own tasks")
    void getAllTasks_FiltersByOwner() throws Exception {
        // User A creates their own task
        Task userATask = Task.builder()
                .title("User A Task")
                .status(TaskStatus.IN_PROGRESS)
                .createdAt(Instant.now())
                .owner(userA)
                .build();
        taskRepository.save(userATask);

        // User A requests all tasks -> should only see User A task (size 1)
        mockMvc.perform(get("/tasks")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("User A Task")))
                .andExpect(jsonPath("$[0].ownerUsername", is("userA")));

        // User B requests all tasks -> should only see User B task (size 1)
        mockMvc.perform(get("/tasks")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("User B Private Task")))
                .andExpect(jsonPath("$[0].ownerUsername", is("userB")));
    }

    @Test
    @DisplayName("User B can fetch and modify their own task (200 OK)")
    void userBCanFetchAndModifyOwnTask() throws Exception {
        mockMvc.perform(get("/tasks/" + userBTask.getId())
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userBTask.getId().intValue())))
                .andExpect(jsonPath("$.title", is("User B Private Task")));

        TaskRequestDto updateRequest = TaskRequestDto.builder()
                .title("Updated by User B")
                .status(TaskStatus.COMPLETED)
                .build();

        mockMvc.perform(put("/tasks/" + userBTask.getId())
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated by User B")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        mockMvc.perform(delete("/tasks/" + userBTask.getId())
                        .header("Authorization", tokenB))
                .andExpect(status().isNoContent());

        assertThat(taskRepository.existsById(userBTask.getId())).isFalse();
    }

    @Test
    @DisplayName("Direct service calls without authentication fail closed with AccessDeniedException")
    void unauthenticatedServiceCall_FailsClosed() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> taskService.getAllTasks())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        assertThatThrownBy(() -> taskService.createTask(TaskRequestDto.builder().title("Unauthenticated").build()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        assertThatThrownBy(() -> taskService.getTaskById(userBTask.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        assertThatThrownBy(() -> taskService.updateTask(userBTask.getId(), TaskRequestDto.builder().title("Hacked").build()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        assertThatThrownBy(() -> taskService.deleteTask(userBTask.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");
    }
}
