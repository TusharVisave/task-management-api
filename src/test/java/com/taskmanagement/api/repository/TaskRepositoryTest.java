package com.taskmanagement.api.repository;

import com.taskmanagement.api.entity.Task;
import com.taskmanagement.api.entity.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should successfully save and retrieve a Task by ID confirming JPA mapping end-to-end")
    void shouldSaveAndRetrieveTask() {
        // Arrange
        Instant dueDate = Instant.now().plus(7, ChronoUnit.DAYS);
        Task task = Task.builder()
                .title("Implement User Authentication")
                .description("Add JWT-based authentication filter and endpoints")
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(dueDate)
                .build();

        // Act - Save and flush to DB, then clear persistence context cache to force real SQL SELECT
        Task savedTask = taskRepository.save(task);
        Long taskId = savedTask.getId();

        entityManager.flush();
        entityManager.clear();

        // Assert - Retrieve from database
        Optional<Task> retrievedOptional = taskRepository.findById(taskId);

        assertThat(retrievedOptional).isPresent();
        Task retrievedTask = retrievedOptional.get();

        assertThat(retrievedTask.getId()).isEqualTo(taskId);
        assertThat(retrievedTask.getTitle()).isEqualTo("Implement User Authentication");
        assertThat(retrievedTask.getDescription()).isEqualTo("Add JWT-based authentication filter and endpoints");
        assertThat(retrievedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(retrievedTask.getCreatedAt()).isNotNull();
        assertThat(retrievedTask.getDueDate()).isNotNull();
    }

    @Test
    @DisplayName("Should filter tasks by status using repository query method")
    void shouldFindTasksByStatus() {
        // Arrange
        Task task1 = Task.builder()
                .title("Task 1")
                .status(TaskStatus.PENDING)
                .build();
        Task task2 = Task.builder()
                .title("Task 2")
                .status(TaskStatus.COMPLETED)
                .build();

        taskRepository.save(task1);
        taskRepository.save(task2);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING);

        // Assert
        assertThat(pendingTasks).hasSize(1);
        assertThat(pendingTasks.getFirst().getTitle()).isEqualTo("Task 1");
        assertThat(pendingTasks.getFirst().getStatus()).isEqualTo(TaskStatus.PENDING);
    }
}
