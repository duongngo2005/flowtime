package com.ndd.flowtime_be.scheduling.dto;

import java.time.Instant;

public record ScheduledBlockSuggestion(
        Long taskId,
        String taskTitle,
        Instant startAt,
        Instant endAt,
        int durationMinutes
) {}
