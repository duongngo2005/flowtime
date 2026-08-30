package com.ndd.flowtime_be.scheduling.dto;

public enum UnscheduledTaskReason {
    DEADLINE_PASSED,
    NO_AVAILABLE_SLOT,
    INSUFFICIENT_DURATION
}
