package com.ndd.flowtime_be.task.service;

import com.ndd.flowtime_be.task.dto.TaskRequest;
import com.ndd.flowtime_be.task.dto.TaskResponse;
import com.ndd.flowtime_be.task.entity.Task;
import com.ndd.flowtime_be.task.entity.TaskPriority;
import com.ndd.flowtime_be.task.entity.TaskStatus;
import com.ndd.flowtime_be.task.repository.TaskRepository;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public TaskResponse create(User user, TaskRequest request) {
        Task task = new Task();
        task.setUser(user);
        applyRequest(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(User user, TaskStatus status, TaskPriority priority, Instant deadline) {
        return taskRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .filter(task -> status == null || task.getStatus() == status)
                .filter(task -> priority == null || task.getPriority() == priority)
                .filter(task -> deadline == null || (task.getDeadline() != null && !task.getDeadline().isAfter(deadline)))
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(User user, Long taskId) {
        return TaskResponse.from(findOwnedTask(user, taskId));
    }

    @Transactional
    public TaskResponse update(User user, Long taskId, TaskRequest request) {
        Task task = findOwnedTask(user, taskId);
        applyRequest(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse complete(User user, Long taskId) {
        Task task = findOwnedTask(user, taskId);
        task.setStatus(TaskStatus.COMPLETED);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(User user, Long taskId) {
        taskRepository.delete(findOwnedTask(user, taskId));
    }

    private Task findOwnedTask(User user, Long taskId) {
        return taskRepository.findByIdAndUser(taskId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));
    }

    private void applyRequest(Task task, TaskRequest request) {
        validatePreferredTime(request);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setEstimatedDuration(request.estimatedDuration());
        task.setPriority(request.priority());
        task.setDeadline(request.deadline());
        task.setPreferredStartTime(request.preferredStartTime());
        task.setPreferredEndTime(request.preferredEndTime());
        task.setMinSessionDuration(request.minSessionDuration());
        task.setSplitAllowed(Boolean.TRUE.equals(request.splitAllowed()));
        task.setCategory(request.category());
    }

    private void validatePreferredTime(TaskRequest request) {
        if (request.preferredStartTime() != null
                && request.preferredEndTime() != null
                && !request.preferredStartTime().isBefore(request.preferredEndTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "preferredStartTime must be before preferredEndTime."
            );
        }
    }
}
