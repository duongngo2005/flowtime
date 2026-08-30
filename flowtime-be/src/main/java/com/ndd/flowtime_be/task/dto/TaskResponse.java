package com.ndd.flowtime_be.task.dto;

import com.ndd.flowtime_be.task.entity.Task;
import com.ndd.flowtime_be.task.entity.TaskPriority;
import com.ndd.flowtime_be.task.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Integer estimatedDuration,
        TaskPriority priority,
        TaskStatus status,
        Instant deadline,
        LocalTime preferredStartTime,
        LocalTime preferredEndTime,
        Integer minSessionDuration,
        Integer maxDailyMinutes,
        boolean splitAllowed,
        String category,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getEstimatedDuration(),
                task.getPriority(),
                task.getStatus(),
                task.getDeadline(),
                task.getPreferredStartTime(),
                task.getPreferredEndTime(),
                task.getMinSessionDuration(),
                task.getMaxDailyMinutes(),
                task.isSplitAllowed(),
                task.getCategory(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
