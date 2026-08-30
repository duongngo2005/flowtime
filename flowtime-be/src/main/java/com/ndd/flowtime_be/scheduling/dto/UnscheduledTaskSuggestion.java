package com.ndd.flowtime_be.scheduling.dto;

public record UnscheduledTaskSuggestion(
        Long taskId,
        String taskTitle,
        int unscheduledMinutes,
        UnscheduledTaskReason reason
) {}
