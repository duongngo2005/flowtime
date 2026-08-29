package com.ndd.flowtime_be.task.controller;

import com.ndd.flowtime_be.task.dto.TaskRequest;
import com.ndd.flowtime_be.task.dto.TaskResponse;
import com.ndd.flowtime_be.task.entity.TaskPriority;
import com.ndd.flowtime_be.task.entity.TaskStatus;
import com.ndd.flowtime_be.task.service.TaskService;
import com.ndd.flowtime_be.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.create(user, request));
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Instant deadline) {
        return ResponseEntity.ok(taskService.list(user, status, priority, deadline));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> get(
            @AuthenticationPrincipal User user,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.get(user, taskId));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.update(user, taskId, request));
    }

    @PatchMapping("/{taskId}/complete")
    public ResponseEntity<TaskResponse> complete(
            @AuthenticationPrincipal User user,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.complete(user, taskId));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long taskId) {
        taskService.delete(user, taskId);
        return ResponseEntity.noContent().build();
    }
}
