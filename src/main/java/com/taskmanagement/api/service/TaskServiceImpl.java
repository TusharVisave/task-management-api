package com.taskmanagement.api.service;

import com.taskmanagement.api.dto.TaskRequestDto;
import com.taskmanagement.api.dto.TaskResponseDto;
import com.taskmanagement.api.entity.Task;
import com.taskmanagement.api.exception.TaskNotFoundException;
import com.taskmanagement.api.mapper.TaskMapper;
import com.taskmanagement.api.repository.TaskRepository;
import com.taskmanagement.api.model.User;
import com.taskmanagement.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TaskResponseDto createTask(TaskRequestDto requestDto) {
        Task task = taskMapper.toEntity(requestDto);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null) {
            task.setOwner(currentUser);
        }
        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponseDto(savedTask);
    }

    @Override
    public List<TaskResponseDto> getAllTasks() {
        User currentUser = getCurrentAuthenticatedUser();
        List<Task> tasks = (currentUser != null)
                ? taskRepository.findAllByOwner(currentUser)
                : taskRepository.findAll();

        return tasks.stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    @Override
    public TaskResponseDto getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);
        return taskMapper.toResponseDto(task);
    }

    @Override
    @Transactional
    public TaskResponseDto updateTask(Long id, TaskRequestDto requestDto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);
        taskMapper.updateEntityFromDto(requestDto, task);
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponseDto(updatedTask);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        checkOwnership(task);
        taskRepository.delete(task);
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    private void checkOwnership(Task task) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && task.getOwner() != null) {
            if (!task.getOwner().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have permission to access or modify this task");
            }
        }
    }
}
