package com.taskmanagement.api.service;

import com.taskmanagement.api.dto.TaskRequestDto;
import com.taskmanagement.api.dto.TaskResponseDto;
import com.taskmanagement.api.entity.Task;
import com.taskmanagement.api.entity.TaskStatus;
import com.taskmanagement.api.exception.TaskNotFoundException;
import com.taskmanagement.api.mapper.TaskMapper;
import com.taskmanagement.api.model.User;
import com.taskmanagement.api.repository.TaskRepository;
import com.taskmanagement.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User testUser;
    private Task task;
    private TaskRequestDto requestDto;
    private TaskResponseDto responseDto;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123", Set.of("ROLE_USER"));
        testUser.setId(1L);

        task = Task.builder()
                .id(1L)
                .title("Complete Day 2")
                .description("Build CRUD and DTO layer")
                .status(TaskStatus.PENDING)
                .createdAt(Instant.now())
                .dueDate(Instant.now().plusSeconds(3600))
                .owner(testUser)
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

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUser.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(userRepository.findByUsername(testUser.getUsername()))
                .thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Fail-closed: clearing SecurityContextHolder causes getAllTasks() and getTaskById() to throw AccessDeniedException without returning data")
    void unauthenticatedCalls_ThrowAccessDeniedException_FailClosed() {
        // Clear security context to simulate an unauthenticated caller reaching the service directly
        SecurityContextHolder.clearContext();

        // 1. Confirm getAllTasks() throws AccessDeniedException and never calls repository findAll()
        assertThatThrownBy(() -> taskService.getAllTasks())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        verify(taskRepository, never()).findAll();
        verify(taskRepository, never()).findAllByOwner(any());

        // 2. Confirm getTaskById() throws AccessDeniedException without returning data
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.getTaskById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        verify(taskMapper, never()).toResponseDto(any());
    }

    @Test
    @DisplayName("createTask should map DTO, associate authenticated owner, save entity and return response DTO")
    void createTask_Success() {
        when(taskMapper.toEntity(requestDto)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponseDto(task)).thenReturn(responseDto);

        TaskResponseDto result = taskService.createTask(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Complete Day 2");
        assertThat(task.getOwner()).isEqualTo(testUser);
        verify(taskMapper).toEntity(requestDto);
        verify(taskRepository).save(task);
        verify(taskMapper).toResponseDto(task);
    }

    @Test
    @DisplayName("createTask should throw AccessDeniedException when caller is unauthenticated (fail-closed)")
    void createTask_Unauthenticated_ThrowsAccessDeniedException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> taskService.createTask(requestDto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllTasks should return list of mapped response DTOs for authenticated user")
    void getAllTasks_Success() {
        when(taskRepository.findAllByOwner(testUser)).thenReturn(List.of(task));
        when(taskMapper.toResponseDto(task)).thenReturn(responseDto);

        List<TaskResponseDto> result = taskService.getAllTasks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Complete Day 2");
        verify(taskRepository).findAllByOwner(testUser);
        verify(taskRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAllTasks should throw AccessDeniedException when caller is unauthenticated (fail-closed)")
    void getAllTasks_Unauthenticated_ThrowsAccessDeniedException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> taskService.getAllTasks())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        verify(taskRepository, never()).findAll();
        verify(taskRepository, never()).findAllByOwner(any());
    }

    @Test
    @DisplayName("getTaskById should return task when found and owned by caller")
    void getTaskById_Found() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponseDto(task)).thenReturn(responseDto);

        TaskResponseDto result = taskService.getTaskById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(taskRepository).findById(1L);
    }

    @Test
    @DisplayName("getTaskById should throw AccessDeniedException when caller is unauthenticated (fail-closed)")
    void getTaskById_Unauthenticated_ThrowsAccessDeniedException() {
        SecurityContextHolder.clearContext();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.getTaskById(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        verify(taskMapper, never()).toResponseDto(any());
    }

    @Test
    @DisplayName("getTaskById should throw AccessDeniedException when task belongs to another user")
    void getTaskById_NotOwner_ThrowsAccessDeniedException() {
        User otherUser = new User("otherUser", "password", Set.of("ROLE_USER"));
        otherUser.setId(2L);
        Task otherUserTask = Task.builder()
                .id(2L)
                .title("Other Task")
                .owner(otherUser)
                .build();

        when(taskRepository.findById(2L)).thenReturn(Optional.of(otherUserTask));

        assertThatThrownBy(() -> taskService.getTaskById(2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access or modify this task");
    }

    @Test
    @DisplayName("getTaskById should throw AccessDeniedException when task has no owner")
    void getTaskById_TaskHasNoOwner_ThrowsAccessDeniedException() {
        Task unownedTask = Task.builder()
                .id(3L)
                .title("Unowned Task")
                .owner(null)
                .build();

        when(taskRepository.findById(3L)).thenReturn(Optional.of(unownedTask));

        assertThatThrownBy(() -> taskService.getTaskById(3L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access or modify this task");
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
                .owner(testUser)
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
    @DisplayName("updateTask should throw AccessDeniedException when caller is unauthenticated (fail-closed)")
    void updateTask_Unauthenticated_ThrowsAccessDeniedException() {
        SecurityContextHolder.clearContext();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateTask(1L, requestDto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTask should throw AccessDeniedException when task belongs to another user")
    void updateTask_NotOwner_ThrowsAccessDeniedException() {
        User otherUser = new User("otherUser", "password", Set.of("ROLE_USER"));
        otherUser.setId(2L);
        Task otherUserTask = Task.builder()
                .id(2L)
                .title("Other Task")
                .owner(otherUser)
                .build();

        when(taskRepository.findById(2L)).thenReturn(Optional.of(otherUserTask));

        assertThatThrownBy(() -> taskService.updateTask(2L, requestDto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access or modify this task");

        verify(taskRepository, never()).save(any());
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
    @DisplayName("deleteTask should delete entity when exists and owned by caller")
    void deleteTask_Found() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.deleteTask(1L);

        verify(taskRepository).findById(1L);
        verify(taskRepository).delete(task);
    }

    @Test
    @DisplayName("deleteTask should throw AccessDeniedException when caller is unauthenticated (fail-closed)")
    void deleteTask_Unauthenticated_ThrowsAccessDeniedException() {
        SecurityContextHolder.clearContext();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.deleteTask(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No authenticated user found");

        verify(taskRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteTask should throw AccessDeniedException when task belongs to another user")
    void deleteTask_NotOwner_ThrowsAccessDeniedException() {
        User otherUser = new User("otherUser", "password", Set.of("ROLE_USER"));
        otherUser.setId(2L);
        Task otherUserTask = Task.builder()
                .id(2L)
                .title("Other Task")
                .owner(otherUser)
                .build();

        when(taskRepository.findById(2L)).thenReturn(Optional.of(otherUserTask));

        assertThatThrownBy(() -> taskService.deleteTask(2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access or modify this task");

        verify(taskRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteTask should throw TaskNotFoundException when task not found")
    void deleteTask_NotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task not found with id: 99");

        verify(taskRepository).findById(99L);
        verify(taskRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getCurrentAuthenticatedUser should throw AccessDeniedException when authenticated username is not in database")
    void getCurrentAuthenticatedUser_UserNotFoundInDb_ThrowsAccessDeniedException() {
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getAllTasks())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No authenticated user found");
    }
}
