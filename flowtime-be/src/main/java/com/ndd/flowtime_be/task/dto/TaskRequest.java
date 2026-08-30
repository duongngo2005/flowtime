package com.ndd.flowtime_be.task.dto;

import com.ndd.flowtime_be.task.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalTime;

public record TaskRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 10_000) String description,
        @NotNull @Positive Integer estimatedDuration,
        @NotNull TaskPriority priority,
        Instant deadline,
        LocalTime preferredStartTime,
        LocalTime preferredEndTime,
        @Positive Integer minSessionDuration,
        @Positive Integer maxDailyMinutes,
        Boolean splitAllowed,
        @Size(max = 100) String category
) {}
