package com.taskmanagement.api.repository;

import com.taskmanagement.api.entity.Task;
import com.taskmanagement.api.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByDueDateBefore(Instant dueDate);
}
