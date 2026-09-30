package com.taskmanagement.api.mapper;

import com.taskmanagement.api.dto.TaskRequestDto;
import com.taskmanagement.api.dto.TaskResponseDto;
import com.taskmanagement.api.entity.Task;
import com.taskmanagement.api.entity.TaskStatus;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus() != null ? dto.getStatus() : TaskStatus.PENDING)
                .dueDate(dto.getDueDate())
                .build();
    }

    public TaskResponseDto toResponseDto(Task entity) {
        if (entity == null) {
            return null;
        }
        return TaskResponseDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .dueDate(entity.getDueDate())
                .ownerUsername(entity.getOwner() != null ? entity.getOwner().getUsername() : null)
                .build();
    }

    public void updateEntityFromDto(TaskRequestDto dto, Task entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        entity.setDueDate(dto.getDueDate());
    }
}
