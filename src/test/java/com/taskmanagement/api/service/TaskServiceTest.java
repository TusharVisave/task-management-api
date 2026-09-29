package com.taskmanagement.api.service;

import com.taskmanagement.api.dto.TaskRequestDto;
import com.taskmanagement.api.dto.TaskResponseDto;
import com.taskmanagement.api.entity.Task;
import com.taskmanagement.api.entity.TaskStatus;
import com.taskmanagement.api.exception.TaskNotFoundException;
import com.taskmanagement.api.mapper.TaskMapper;
import com.taskmanagement.api.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task task;
    private TaskRequestDto requestDto;
    private TaskResponseDto responseDto;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(1L)
                .title("Complete Day 2")
                .description("Build CRUD and DTO layer")
                .status(TaskStatus.PENDING)
                .createdAt(Instant.now())
                .dueDate(Instant.now().plusSeconds(3600))
                .build();

        requestDto = TaskRequestDto.builder()
                .title("Complete Day 2")
                .description("Build CRUD and DTO layer")
                .status(TaskStatus.PENDING)
                .dueDate(task.getDueDate())
                .build();

        responseDto = TaskResponseDto.builder()
                .id(1L)
                .title("Complete Day 2")
                .description("Build CRUD and DTO layer")
                .status(TaskStatus.PENDING)
                .createdAt(task.getCreatedAt())
                .dueDate(task.getDueDate())
                .build();
    }

    @Test
    @DisplayName("createTask should map DTO, save entity and return response DTO")
    void createTask_Success() {
        when(taskMapper.toEntity(requestDto)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponseDto(task)).thenReturn(responseDto);

        TaskResponseDto result = taskService.createTask(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Complete Day 2");
        verify(taskMapper).toEntity(requestDto);
        verify(taskRepository).save(task);
        verify(taskMapper).toResponseDto(task);
    }

    @Test
    @DisplayName("getAllTasks should return list of mapped response DTOs")
    void getAllTasks_Success() {
        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(taskMapper.toResponseDto(task)).thenReturn(responseDto);

        List<TaskResponseDto> result = taskService.getAllTasks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Complete Day 2");
        verify(taskRepository).findAll();
    }

    @Test
    @DisplayName("getTaskById should return task when found")
    void getTaskById_Found() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponseDto(task)).thenReturn(responseDto);

        TaskResponseDto result = taskService.getTaskById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(taskRepository).findById(1L);
    }

    @Test
    @DisplayName("getTaskById should throw TaskNotFoundException when id is missing")
    void getTaskById_NotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task not found with id: 99");

        verify(taskRepository).findById(99L);
        verify(taskMapper, never()).toResponseDto(any());
    }

    @Test
    @DisplayName("updateTask should update existing entity and return updated DTO")
    void updateTask_Found() {
        TaskRequestDto updateRequest = TaskRequestDto.builder()
                .title("Updated Title")
                .description("Updated Description")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        Task updatedTask = Task.builder()
                .id(1L)
                .title("Updated Title")
                .description("Updated Description")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        TaskResponseDto updatedResponse = TaskResponseDto.builder()
                .id(1L)
                .title("Updated Title")
                .description("Updated Description")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(updatedTask);
        when(taskMapper.toResponseDto(updatedTask)).thenReturn(updatedResponse);

        TaskResponseDto result = taskService.updateTask(1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskMapper).updateEntityFromDto(updateRequest, task);
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("updateTask should throw TaskNotFoundException when task not found")
    void updateTask_NotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(99L, requestDto))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task not found with id: 99");

        verify(taskRepository).findById(99L);
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteTask should delete entity when exists")
    void deleteTask_Found() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteTask(1L);

        verify(taskRepository).existsById(1L);
        verify(taskRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTask should throw TaskNotFoundException when task not found")
    void deleteTask_NotFound() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task not found with id: 99");

        verify(taskRepository).existsById(99L);
        verify(taskRepository, never()).deleteById(any());
    }
}
