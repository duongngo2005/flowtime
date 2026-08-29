package com.ndd.flowtime_be.scheduling.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SchedulingPreviewResponse(
        LocalDate startDate,
        LocalDate endDate,
        String timezone,
        Instant generatedAt,
        List<ScheduledBlockSuggestion> suggestions,
        List<UnscheduledTaskSuggestion> unscheduledTasks
) {}
