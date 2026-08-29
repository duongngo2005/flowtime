package com.ndd.flowtime_be.task.service;

import com.ndd.flowtime_be.task.dto.TaskRequest;
import com.ndd.flowtime_be.task.dto.TaskResponse;
import com.ndd.flowtime_be.task.entity.Task;
import com.ndd.flowtime_be.task.entity.TaskPriority;
import com.ndd.flowtime_be.task.entity.TaskStatus;
import com.ndd.flowtime_be.task.repository.TaskRepository;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private final User user = User.builder().id(1L).email("user@example.com").name("Test User").build();

    @Test
    void createsTodoTaskWithRequestedSchedulingFields() {
        TaskRequest request = request("Finish thesis", TaskPriority.HIGH, Instant.parse("2026-09-05T16:59:00Z"));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(user, request);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        assertEquals(TaskStatus.TODO, response.status());
        assertEquals("Finish thesis", taskCaptor.getValue().getTitle());
        assertEquals(120, taskCaptor.getValue().getEstimatedDuration());
        assertEquals(TaskPriority.HIGH, taskCaptor.getValue().getPriority());
        assertFalse(taskCaptor.getValue().isSplitAllowed());
    }

    @Test
    void filtersTasksByStatusPriorityAndDeadline() {
        Instant filterDeadline = Instant.parse("2026-09-05T16:59:00Z");
        Task matchingTask = task(1L, "Urgent task", TaskStatus.TODO, TaskPriority.HIGH, filterDeadline);
        Task completedTask = task(2L, "Completed task", TaskStatus.COMPLETED, TaskPriority.HIGH, filterDeadline);
        Task laterTask = task(3L, "Later task", TaskStatus.TODO, TaskPriority.HIGH,
                Instant.parse("2026-09-10T16:59:00Z"));
        when(taskRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(matchingTask, completedTask, laterTask));

        List<TaskResponse> tasks = taskService.list(user, TaskStatus.TODO, TaskPriority.HIGH, filterDeadline);

        assertEquals(List.of("Urgent task"), tasks.stream().map(TaskResponse::title).toList());
    }

    @Test
    void completesOnlyOwnedTask() {
        Task task = task(1L, "Finish thesis", TaskStatus.TODO, TaskPriority.HIGH, null);
        when(taskRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.complete(user, 1L);

        assertEquals(TaskStatus.COMPLETED, response.status());
        verify(taskRepository).save(task);
    }

    @Test
    void hidesTaskBelongingToAnotherUser() {
        when(taskRepository.findByIdAndUser(2L, user)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.get(user, 2L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private TaskRequest request(String title, TaskPriority priority, Instant deadline) {
        return new TaskRequest(
                title,
                "Important work",
                120,
                priority,
                deadline,
                LocalTime.of(18, 0),
                LocalTime.of(22, 0),
                60,
                false,
                "School"
        );
    }

    private Task task(Long id, String title, TaskStatus status, TaskPriority priority, Instant deadline) {
        return Task.builder()
                .id(id)
                .user(user)
                .title(title)
                .estimatedDuration(120)
                .priority(priority)
                .status(status)
                .deadline(deadline)
                .splitAllowed(false)
                .build();
    }
}
