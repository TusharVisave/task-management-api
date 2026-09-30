package com.taskmanagement.api.dto;

import com.taskmanagement.api.entity.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequestDto {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private TaskStatus status;

    @FutureOrPresent(message = "Due date must be today or in the future")
    private Instant dueDate;
}
