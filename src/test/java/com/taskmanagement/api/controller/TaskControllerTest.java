package com.taskmanagement.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.api.dto.TaskRequestDto;
import com.taskmanagement.api.dto.TaskResponseDto;
import com.taskmanagement.api.entity.TaskStatus;
import com.taskmanagement.api.exception.GlobalExceptionHandler;
import com.taskmanagement.api.exception.TaskNotFoundException;
import com.taskmanagement.api.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    // ─────────────────────────────────────────────────────────────────────────
    // Happy-path CRUD tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /tasks should return 201 Created and response DTO")
    void createTask_Returns201() throws Exception {
        TaskRequestDto request = TaskRequestDto.builder()
                .title("Learn DTOs")
                .description("Understand why entities should not be returned directly")
                .status(TaskStatus.PENDING)
                .dueDate(Instant.parse("2026-10-01T10:00:00Z"))
                .build();

        TaskResponseDto response = TaskResponseDto.builder()
                .id(1L)
                .title("Learn DTOs")
                .description("Understand why entities should not be returned directly")
                .status(TaskStatus.PENDING)
                .createdAt(Instant.parse("2026-09-29T10:00:00Z"))
                .dueDate(Instant.parse("2026-10-01T10:00:00Z"))
                .build();

        when(taskService.createTask(any(TaskRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Learn DTOs")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.description", is("Understand why entities should not be returned directly")));
    }

    @Test
    @DisplayName("GET /tasks should return 200 OK and list of tasks")
    void getAllTasks_Returns200() throws Exception {
        TaskResponseDto response = TaskResponseDto.builder()
                .id(1L)
                .title("Test Task")
                .status(TaskStatus.PENDING)
                .build();

        when(taskService.getAllTasks()).thenReturn(List.of(response));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Task")));
    }

    @Test
    @DisplayName("GET /tasks/{id} should return 200 OK when found")
    void getTaskById_Found_Returns200() throws Exception {
        TaskResponseDto response = TaskResponseDto.builder()
                .id(1L)
                .title("Test Task")
                .status(TaskStatus.PENDING)
                .build();

        when(taskService.getTaskById(1L)).thenReturn(response);

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Task")));
    }

    @Test
    @DisplayName("GET /tasks/{id} should return 404 Not Found when task does not exist")
    void getTaskById_NotFound_Returns404() throws Exception {
        when(taskService.getTaskById(999L)).thenThrow(new TaskNotFoundException(999L));

        mockMvc.perform(get("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Task not found with id: 999")));
    }

    @Test
    @DisplayName("PUT /tasks/{id} should return 200 OK when updated successfully")
    void updateTask_Found_Returns200() throws Exception {
        TaskRequestDto updateRequest = TaskRequestDto.builder()
                .title("Updated Task Title")
                .description("Updated details")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        TaskResponseDto updatedResponse = TaskResponseDto.builder()
                .id(1L)
                .title("Updated Task Title")
                .description("Updated details")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        when(taskService.updateTask(eq(1L), any(TaskRequestDto.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Updated Task Title")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("PUT /tasks/{id} should return 404 Not Found when task does not exist")
    void updateTask_NotFound_Returns404() throws Exception {
        TaskRequestDto updateRequest = TaskRequestDto.builder()
                .title("Updated Task Title")
                .build();

        when(taskService.updateTask(eq(999L), any(TaskRequestDto.class)))
                .thenThrow(new TaskNotFoundException(999L));

        mockMvc.perform(put("/tasks/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Task not found with id: 999")));
    }

    @Test
    @DisplayName("DELETE /tasks/{id} should return 204 No Content when deleted")
    void deleteTask_Found_Returns204() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /tasks/{id} should return 404 Not Found when missing")
    void deleteTask_NotFound_Returns404() throws Exception {
        doThrow(new TaskNotFoundException(999L)).when(taskService).deleteTask(999L);

        mockMvc.perform(delete("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Task not found with id: 999")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation tests – 400 Bad Request scenarios
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /tasks with blank title should return 400 with validation error")
    void createTask_BlankTitle_Returns400() throws Exception {
        TaskRequestDto invalidRequest = TaskRequestDto.builder()
                .title("")
                .description("No title provided")
                .build();

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.validationErrors.title", is("Title cannot be blank")));
    }

    @Test
    @DisplayName("POST /tasks with null title should return 400 with validation error")
    void createTask_NullTitle_Returns400() throws Exception {
        // Sending a body with no title field at all
        String body = "{\"description\":\"missing title\"}";

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    @DisplayName("POST /tasks with title exceeding 200 chars should return 400 with validation error")
    void createTask_TitleTooLong_Returns400() throws Exception {
        String longTitle = "A".repeat(201);
        TaskRequestDto invalidRequest = TaskRequestDto.builder()
                .title(longTitle)
                .build();

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.validationErrors.title",
                        is("Title must not exceed 200 characters")));
    }

    @Test
    @DisplayName("POST /tasks with past due date should return 400 with validation error")
    void createTask_PastDueDate_Returns400() throws Exception {
        Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
        TaskRequestDto invalidRequest = TaskRequestDto.builder()
                .title("Valid Title")
                .dueDate(pastDate)
                .build();

        mockMvc.perform(post("/tasks")
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
    @DisplayName("POST /tasks with malformed JSON should return 400 with readable message")
    void createTask_MalformedJson_Returns400() throws Exception {
        String malformedJson = "{ title: not valid json }";

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Malformed JSON request body")));
    }

    @Test
    @DisplayName("PUT /tasks/{id} with blank title should return 400 with validation error")
    void updateTask_BlankTitle_Returns400() throws Exception {
        TaskRequestDto invalidRequest = TaskRequestDto.builder()
                .title("  ")
                .build();

        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.validationErrors.title", is("Title cannot be blank")));
    }
}

