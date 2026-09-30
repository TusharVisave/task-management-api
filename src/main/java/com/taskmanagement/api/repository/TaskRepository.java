package com.taskmanagement.api.repository;

import com.taskmanagement.api.entity.Task;
import com.taskmanagement.api.entity.TaskStatus;
import com.taskmanagement.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByDueDateBefore(Instant dueDate);

    List<Task> findAllByOwner(User owner);

    List<Task> findAllByOwnerUsername(String username);

    Optional<Task> findByIdAndOwner(Long id, User owner);

    Optional<Task> findByIdAndOwnerUsername(Long id, String username);
}

